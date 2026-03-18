package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.domain.auth.component.GoogleTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.KakaoTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.NaverTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.OAuthUserInfo;
import com.buddy.buddyapi.domain.auth.dto.AuthDto;
import com.buddy.buddyapi.domain.auth.enums.AuthStatus;
import com.buddy.buddyapi.domain.member.*;
import com.buddy.buddyapi.domain.member.event.MemberWithdrawEvent;
import com.buddy.buddyapi.domain.member.event.SocialUnlinkEvent;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OauthService {

    private final OauthAccountRepository oauthAccountRepository;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

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
     * 각 소셜 제공자별 알맞은 토큰 검증기(Verifier)를 호출하여 유저 정보를 추출합니다.
     */
    public OAuthUserInfo verifyOauthToken(String provider, String token) {
        return switch (provider.toLowerCase()) {
            case "google" -> googleTokenVerifier.verify(token);
            case "kakao" -> kakaoTokenVerifier.verify(token);
            case "naver" -> naverTokenVerifier.verify(token);
            default -> throw new BaseException(ResultCode.UNSUPPORTED_PROVIDER);
        };
    }

    /**
     * 연동 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isLinked(Member member, Provider provider) {
        return oauthAccountRepository.existsByMemberAndProvider(member, provider);
    }

    /**
     * 소셜 토큰 갱신
     */
    @Transactional
    public void updateSocialTokens(Member member, Provider provider, String accessToken, String refreshToken) {
        OauthAccount account = oauthAccountRepository.findByMemberAndProvider(member, provider)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
        account.updateTokens(accessToken, refreshToken);
    }

    /**
     * 4. OauthAccount DB 저장
     */
    @Transactional
    public void saveOauthAccount(Member member, Provider provider, String oauthId, String accessToken, String refreshToken) {
        oauthAccountRepository.save(OauthAccount.builder()
                .provider(provider)
                .oauthId(oauthId)
                .socialAccessToken(accessToken)
                .socialRefreshToken(refreshToken)
                .member(member)
                .build());
    }

    /**
     * 연동 필요 상태(REQUIRES_LINKING) Redis 임시 저장 및 응답 생성
     * 소셜 연동이 필요한 유저의 정보를 Redis에 10분간 임시 보관하고, 프론트엔드에 REQUIRES_LINKING 상태를 반환합니다.
     */
    public AuthDto.LoginResponse handleLinkingRequired(String provider, OAuthUserInfo userInfo) throws JsonProcessingException {
        String linkKey = UUID.randomUUID().toString();
        OAuthLinkInfo linkInfo = OAuthLinkInfo.builder()
                .email(userInfo.email())
                .provider(provider)
                .oauthId(userInfo.oauthId())
                .socialAccessToken(userInfo.socialAccessToken())
                .socialRefreshToken(userInfo.socialRefreshToken())
                .build();

        redisTemplate.opsForValue().set("OAUTH_LINK:" + linkKey,
                objectMapper.writeValueAsString(linkInfo), Duration.ofMinutes(10));

        return AuthDto.LoginResponse.builder()
                .status(AuthStatus.REQUIRES_LINKING)
                .linkKey(linkKey)
                .build();
    }

    /**
     * Redis에서 연동 정보(LinkInfo) 꺼내기
     */
    public OAuthLinkInfo getAndRemoveLinkInfo(String linkKey) throws JsonProcessingException {
        String redisKey = "OAUTH_LINK:" + linkKey;
        String jsonValue = redisTemplate.opsForValue().getAndDelete(redisKey);

        if (jsonValue == null) {
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
        return objectMapper.readValue(jsonValue, OAuthLinkInfo.class);
    }

    /**
     * [트랜잭션 내부, 즉시 실행]
     * 소셜 계정 정보를 DB에서 읽어 스냅샷을 만들고, AFTER_COMMIT 처리 이벤트를 예약합니다.
     * 실제 외부 API 호출은 트랜잭션 밖(AFTER_COMMIT)에서 수행해 DB 커넥션 점유를 최소화합니다.
     */
    @EventListener
    @Transactional(readOnly = true)
    public void onMemberWithdraw(MemberWithdrawEvent event) {
        List<OauthAccount> accounts = oauthAccountRepository.findByMember_MemberId(event.memberId());

        if (accounts.isEmpty()) {
            log.info("📢 [OauthService] 탈퇴 유저(Id: {})의 소셜 연동 없음.", event.memberId());
            return;
        }

        List<SocialUnlinkEvent.AccountSnapshot> snapshots = accounts.stream()
                .map(a -> new SocialUnlinkEvent.AccountSnapshot(
                        a.getProvider(),
                        a.getOauthId(),
                        a.getSocialAccessToken(),
                        a.getSocialRefreshToken()
                ))
                .toList();

        eventPublisher.publishEvent(new SocialUnlinkEvent(snapshots));
        log.info("📢 [OauthService] 탈퇴 유저(Id: {})의 소셜 연동 해제 이벤트 예약 ({}개 계정)",
                event.memberId(), snapshots.size());
    }

    /**
     * [트랜잭션 커밋 후 실행]
     * DB 커밋이 확정된 뒤 소셜 연동 해제 API를 호출합니다.
     * 개별 실패가 다른 제공자 처리를 막지 않도록 예외를 개별 흡수합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void unlinkAfterCommit(SocialUnlinkEvent event) {
        event.accounts().forEach(account -> {
            try {
                switch (account.provider()) {
                    case KAKAO  -> unlinkKakao(account.oauthId());
                    case NAVER  -> unlinkNaver(account.accessToken(), account.refreshToken());
                    case GOOGLE -> log.info("구글 연동은 프론트에서 처리합니다.");
                }
            } catch (Exception e) {
                // 연동 해제 실패는 치명적이지 않음. 로깅 후 계속 진행.
                log.error("🔴 [OauthService] 소셜 연결 끊기 실패: provider={}, oauthId={}",
                        account.provider(), account.oauthId(), e);
            }
        });
        log.info("📢 [OauthService] 소셜 연결 해제 처리 완료");
    }

    // =========================================================================
    // 헬퍼 메서드 (Helper Methods)
    // =========================================================================

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

}
