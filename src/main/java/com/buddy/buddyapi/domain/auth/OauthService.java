package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.domain.auth.component.GoogleTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.KakaoTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.NaverTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.OAuthUserInfo;
import com.buddy.buddyapi.domain.auth.dto.LoginResponse;
import com.buddy.buddyapi.domain.auth.dto.OAuthDto;
import com.buddy.buddyapi.domain.member.*;
import com.buddy.buddyapi.domain.member.dto.MemberResponse;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.global.security.JwtTokenProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OauthService {

    private final MemberRepository memberRepository;
    private final OauthAccountRepository oauthAccountRepository;

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final GoogleTokenVerifier googleTokenVerifier;
    private final KakaoTokenVerifier kakaoTokenVerifier;
    private final NaverTokenVerifier naverTokenVerifier;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${kakao.admin-key}")
    private String kakaoAdminKey;

    /**
     * [소셜 로그인 통합] 제공자(Google, Kakao, Naver)의 토큰을 검증하고 서비스 로그인을 처리합니다.
     * 이미 가입된 이메일이 존재하지만 소셜 연동이 되어있지 않은 경우, 연동 대기 상태(REQUIRES_LINKING)를 반환합니다.
     *
     * @param request 제공자 이름(provider)과 인증 토큰(token)
     * @return 로그인 성공(SUCCESS) 또는 연동 필요(REQUIRES_LINKING) 상태가 포함된 응답 DTO
     */
    @Transactional
    public LoginResponse socialLogin(OAuthDto.LoginRequest request) throws JsonProcessingException {

        OAuthUserInfo userInfo = verifyOauthToken(request.provider(), request.code());
        Provider provider = Provider.from(request.provider());

        // 기존 회원 여부 확인
        Optional<Member> optionalMember = memberRepository.findByEmail(userInfo.email());

        // [CASE] 이미 가입된 계정이 있는 경우
        if(optionalMember.isPresent()) {
            Member member = optionalMember.get();

            // [CASE] 연동이 필요한 경우 (REQUIRES_LINKING)
            if(!oauthAccountRepository.existsByMemberAndProvider(member, provider)) {
                return handleLinkingRequired(request, userInfo);
            }

            // [CASE] 이미 연동됨 -> 로그인 성공 (SUCCESS)
            OauthAccount account = oauthAccountRepository.findByMemberAndProvider(member, provider)
                    .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
            account.updateTokens(userInfo.socialAccessToken(), userInfo.socialRefreshToken());

            return buildAuthResponse(member, AuthStatus.SUCCESS);
        }

        // [CASE] 아예 신규 유저 (가입 + 연동 + SUCCESS)
        Member newMember = memberRepository.save(Member.builder()
                .email(userInfo.email())
                .nickname(userInfo.name())
                .build());

        oauthAccountRepository.save(OauthAccount.builder()
                .provider(provider)
                .oauthId(userInfo.oauthId())
                .socialAccessToken(userInfo.socialAccessToken())
                .socialRefreshToken(userInfo.socialRefreshToken())
                .member(newMember)
                .build());

        return buildAuthResponse(newMember, AuthStatus.SUCCESS);

    }

    /**
     * [소셜 계정 연동 완료] Redis에 임시 저장된 연동 정보를 확인하고, 기존 계정에 새로운 소셜 정보를 연동합니다.
     *
     * @param key 연동 대기 상태에서 프론트엔드로 전달했던 임시 키 (linkKey)
     * @return 연동 완료 후 발급된 토큰셋을 포함한 로그인 성공 응답
     * @throws BaseException 키가 만료되었거나 조작된 경우 발생
     */
    @Transactional
    public LoginResponse linkOauthAccount(String key) throws JsonProcessingException {

        // Redis에서 검증된 진짜 정보 꺼내기
        String redisKey = "OAUTH_LINK:" + key;
        String jsonValue = redisTemplate.opsForValue().getAndDelete(redisKey);

        if (jsonValue == null) {
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }

        OAuthLinkInfo linkInfo = objectMapper.readValue(jsonValue, OAuthLinkInfo.class);

        Member member = memberRepository.findByEmail(linkInfo.email())
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

        Provider provider = Provider.from(linkInfo.provider());

        if (oauthAccountRepository.existsByMemberAndProvider(member, provider)) {
            throw new BaseException(ResultCode.ALREADY_LINKED_ACCOUNT);
        }

        oauthAccountRepository.save(OauthAccount.builder()
                .provider(provider)
                .oauthId(linkInfo.oauthId())
                .socialAccessToken(linkInfo.socialAccessToken())
                .socialRefreshToken(linkInfo.socialRefreshToken())
                .member(member)
                .build());

        return buildAuthResponse(member, AuthStatus.SUCCESS);

    }

    @Transactional
    public void unlinkSocialAccounts(Long memberSeq) {
        List<OauthAccount> linkedAccounts = oauthAccountRepository.findByMember_MemberSeq(memberSeq);

        for (OauthAccount account : linkedAccounts) {
            String dbAccessToken = account.getSocialAccessToken();
            String dbRefreshToken = account.getSocialRefreshToken();

            switch (account.getProvider()) {
                case KAKAO -> unlinkKakao(account.getOauthId());
                case GOOGLE -> unlinkGoogle(dbRefreshToken);
                case NAVER -> unlinkNaver(dbAccessToken, dbRefreshToken);
            }
        }
    }

    // =========================================================================
    // 헬퍼 메서드 (Helper Methods)
    // =========================================================================

    /**
     * 각 소셜 제공자별 알맞은 토큰 검증기(Verifier)를 호출하여 유저 정보를 추출합니다.
     */
    private OAuthUserInfo verifyOauthToken(String provider, String token) {
        return switch (provider.toLowerCase()) {
            case "google" -> googleTokenVerifier.verify(token);
            case "kakao" -> kakaoTokenVerifier.verify(token);
            case "naver" -> naverTokenVerifier.verify(token);
            default -> throw new BaseException(ResultCode.UNSUPPORTED_PROVIDER);
        };
    }

    /**
     * 소셜 연동이 필요한 유저의 정보를 Redis에 10분간 임시 보관하고, 프론트엔드에 REQUIRES_LINKING 상태를 반환합니다.
     */
    private LoginResponse handleLinkingRequired(OAuthDto.LoginRequest request, OAuthUserInfo userInfo) throws JsonProcessingException {
        String linkKey = UUID.randomUUID().toString();
        OAuthLinkInfo linkInfo = OAuthLinkInfo.builder()
                .email(userInfo.email())
                .provider(request.provider())
                .oauthId(userInfo.oauthId())
                .socialAccessToken(userInfo.socialAccessToken())
                .socialRefreshToken(userInfo.socialRefreshToken())
                .build();

        redisTemplate.opsForValue().set("OAUTH_LINK:" + linkKey,
                objectMapper.writeValueAsString(linkInfo), Duration.ofMinutes(10));

        return LoginResponse.builder()
                .status(AuthStatus.REQUIRES_LINKING)
                .linkKey(linkKey)
                .build();
    }

    /**
     * 카카오 연결 끊기 (Admin Key 방식)
     */
    private void unlinkKakao(String oauthId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String reqURL = "https://kapi.kakao.com/v1/user/unlink";

            // 1. 헤더 설정 (Admin Key 필수)
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "KakaoAK " + kakaoAdminKey);
            headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            // 2. 바디 설정 (누구의 연결을 끊을 것인가?)
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("target_id_type", "user_id");
            body.add("target_id", oauthId);

            // 3. 요청 쏘기
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(reqURL, request, String.class);

            log.info("🟢 카카오 연결 끊기 성공: {}", response.getBody());
        } catch (Exception e) {
            log.error("🔴 카카오 연결 끊기 실패: oauthId={}, 원인={}", oauthId, e.getMessage());
        }
    }

    /**
     *
     */
    private void unlinkGoogle(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("🟡 구글 연결 끊기 실패: Access Token이 없습니다.");
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String reqURL = "https://oauth2.googleapis.com/revoke";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", accessToken); // 구글은 token 하나만 던져주면 됩니다.

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(reqURL, request, String.class);

            log.info("🟢 구글 연결 끊기 성공 (Status: {})", response.getStatusCode());
        } catch (Exception e) {
            log.error("🔴 구글 연결 끊기 실패 원인: {}", e.getMessage());
        }
    }

    /**
     *
     */
    private void unlinkNaver(String accessToken, String refreshToken) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("🟡 네이버 연결 끊기 실패: Access Token이 없습니다.");
            return;
        }

        try {
            // 1. 일단 가지고 있는 Access Token으로 끊어봅니다.
            boolean isSuccess = sendNaverDeleteRequest(accessToken);

            // 2. 만약 실패했다면? (아마 토큰 만료일 확률이 99%) -> 리프레시 토큰으로 심폐소생술!
            if (!isSuccess && refreshToken != null && !refreshToken.isBlank()) {
                log.info("🟡 네이버 Access Token 만료 추정. Refresh Token으로 갱신을 시도합니다.");
                String newAccessToken = refreshNaverToken(refreshToken);

                if (newAccessToken != null) {
                    sendNaverDeleteRequest(newAccessToken); // 새 토큰으로 다시 끊기 요청!
                }
            }
        } catch (Exception e) {
            log.error("🔴 네이버 연결 끊기 최종 실패 원인: {}", e.getMessage());
        }
    }

    /**
     * 네이버 실제 삭제 API 찌르기 (내부 헬퍼)
     */
    private boolean sendNaverDeleteRequest(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String reqURL = "https://nid.naver.com/oauth2.0/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "delete");
            body.add("client_id", naverClientId);
            body.add("client_secret", naverClientSecret);
            body.add("access_token", accessToken);
            body.add("service_provider", "NAVER");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(reqURL, request, String.class);

            // HTTP 200 이면 성공!
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("🟢 네이버 연결 끊기 성공!");
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 네이버 토큰 심폐소생술 (토큰 갱신 API)
     */
    private String refreshNaverToken(String refreshToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String reqURL = "https://nid.naver.com/oauth2.0/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token"); // 💡 갱신 요청!
            body.add("client_id", naverClientId);
            body.add("client_secret", naverClientSecret);
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(reqURL, request, JsonNode.class);

            // 갱신된 따끈따끈한 새 액세스 토큰 반환!
            return response.getBody().path("access_token").asText(null);
        } catch (Exception e) {
            log.error("🔴 네이버 토큰 갱신 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 공통 응답 생성 로직 (OauthService 용)
     */
    private LoginResponse buildAuthResponse(Member member, AuthStatus status) {

        AuthStatus finalStatus = status;
        if (status == AuthStatus.SUCCESS && member.getBuddyCharacter() == null) {
            finalStatus = AuthStatus.REQUIRES_CHARACTER;
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getMemberSeq());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberSeq());

        return LoginResponse.builder()
                // 🚨 주의: 기존 코드에 .status(status) 로 되어있었습니다!
                // 그래서 캐릭터가 없어도 무조건 SUCCESS가 나가는 버그가 있었어요.
                // 이걸 finalStatus로 바꿔야 캐릭터 유무에 따라 상태가 제대로 바뀝니다!
                .status(finalStatus)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .member(MemberResponse.from(member))
                .build();
    }


}
