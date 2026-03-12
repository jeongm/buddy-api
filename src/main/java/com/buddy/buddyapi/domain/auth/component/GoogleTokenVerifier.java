package com.buddy.buddyapi.domain.auth.component;

import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class GoogleTokenVerifier {

    private final RestTemplate restTemplate;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}") // RN에서 설정한 redirect_uri
    private String redirectUri;

    /**
     * 프론트에서 넘어온 id_token을 검증하고, 유저 정보(이메일, 이름)를 반환
     */
    public OAuthUserInfo verify(String code) {
        // 1. 구글로부터 토큰 세트(Access, Refresh) 발급
        OAuthTokenResponse tokenResponse = getTokens(code);

        // 2. 액세스 토큰으로 유저 프로필 조회
        GoogleUserResponse profile = fetchUserProfile(tokenResponse.accessToken());

        // 3. 최종 조립 (네이버/카카오와 완벽히 동일한 결!)
        return new OAuthUserInfo(
                profile.email(),
                profile.name(),
                profile.sub(), // 구글의 고유 ID는 sub 필드에 있습니다.
                tokenResponse.accessToken(),
                tokenResponse.refreshToken()
        );
    }

    private OAuthTokenResponse getTokens(String code) {
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
            ResponseEntity<OAuthTokenResponse> response = restTemplate.postForEntity(
                    GOOGLE_TOKEN_URL, request, OAuthTokenResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("구글 토큰 발급 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
    }

    private GoogleUserResponse fetchUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleUserResponse> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL, HttpMethod.GET, entity, GoogleUserResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("구글 API 통신 에러 (HTTP 상태 코드 문제): {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        } catch (Exception e) {
            log.error("구글 토큰 처리 중 알 수 없는 서버 에러 발생: ", e);
            throw new BaseException(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    private record GoogleUserResponse(
            String sub,
            String email,
            String name,
            @JsonProperty("email_verified") boolean emailVerified
    ) {}
}
