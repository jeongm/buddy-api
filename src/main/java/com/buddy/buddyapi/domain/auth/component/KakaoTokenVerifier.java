package com.buddy.buddyapi.domain.auth.component;

import com.buddy.buddyapi.domain.auth.dto.OAuthTokenResponse;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.annotation.JsonProperty;
// 💡 ObjectMapper랑 JsonNode는 이제 영원히 안녕입니다!
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoTokenVerifier {

    private final RestTemplate restTemplate;
    // 💡 생성자에서 ObjectMapper 제거했습니다!

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    /**
     * [감독관] 프론트에서 넘어온 인가 코드를 검증하고, 최종 유저 정보를 반환
     */
    public OAuthUserInfo verify(String code) {

        OAuthTokenResponse tokenResponse = getTokens(code);

        KakaoUserResponse profile = fetchUserProfile(tokenResponse.accessToken());

        // 닉네임이 없을 경우를 대비한 안전한 추출 (카카오는 가끔 프로필이 null일 수 있습니다)
        String nickname = (profile.kakaoAccount().profile() != null)
                ? profile.kakaoAccount().profile().nickname()
                : profile.kakaoAccount().email();

        // id는 Long이라 String으로 변환
        return new OAuthUserInfo(
                profile.kakaoAccount().email(),
                nickname,
                String.valueOf(profile.id()),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken()
        );
    }

    /**
     * 인가 코드로 카카오 액세스 토큰 '객체' 발급
     */
    private OAuthTokenResponse getTokens(String code){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<OAuthTokenResponse> response = restTemplate.postForEntity(KAKAO_TOKEN_URL, request, OAuthTokenResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("카카오 토큰 발급 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
    }

    /**
     * 발급받은 토큰으로 유저 정보 조회
     */
    private KakaoUserResponse fetchUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            // 카카오 전용 DTO로 한 방에 받기!
            ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL, HttpMethod.GET, entity, KakaoUserResponse.class
            );

            KakaoUserResponse responseBody = response.getBody();

            // 💡 널(Null) 체크 방어 로직!
            if(responseBody == null || responseBody.kakaoAccount() == null || responseBody.kakaoAccount().email() == null) {
                log.error("카카오 이메일 제공 동의가 필요합니다.");
                throw new BaseException(ResultCode.INVALID_TOKEN);
            }

            // 원본 객체 그대로 던져주기 (SocialProfile은 잊으세요!)
            return responseBody;

        } catch (RestClientException e) {
            // 네이버 서버가 터졌거나, 파라미터가 틀렸을 때 (HTTP 에러)
            log.error("카카오 API 통신 에러 (HTTP 상태 코드 문제): {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        } catch (Exception e) {
            // 그 외의 예상치 못한 런타임 에러 (NullPointer 등)
            log.error("카카오 토큰 처리 중 알 수 없는 서버 에러 발생: ", e); // e를 넘겨서 스택 트레이스 전체 출력
            throw new BaseException(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 💡 카카오 JSON 구조에 딱 맞춘 프라이빗 DTO
    private record KakaoUserResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
        public record KakaoAccount(String email, Profile profile) {}
        public record Profile(String nickname) {}
    }
}