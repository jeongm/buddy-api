package com.buddy.buddyapi.domain.auth.component;

import com.buddy.buddyapi.domain.auth.dto.OAuthTokenResponse;
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
public class NaverTokenVerifier {

    private final RestTemplate restTemplate;

    // 네이버 API 주소들
    private static final String NAVER_TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String clientSecret;

    /**
     * 프론트에서 넘어온 '인가 코드(Code)'로 토큰을 발급받고, 유저 정보를 반환
     */
    public OAuthUserInfo verify(String code) {
        OAuthTokenResponse tokenResponse = getTokens(code);

        NaverUserResponse.Profile profile = fetchUserProfile(tokenResponse.accessToken());

        return new OAuthUserInfo(
                profile.email(),
                profile.name(),
                profile.id(),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken()
        );
    }

    /**
     * [Step 1] 인가 코드로 네이버 액세스 토큰 발급 (신규 추가)
     */
    private OAuthTokenResponse getTokens(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        // 네이버 특유의 필수 파라미터 (프론트에서 url 띄울 때 썼던 state 값과 동일하거나 임의의 문자열)
        params.add("state", "BUDDY_STATE");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<OAuthTokenResponse> response = restTemplate.postForEntity(NAVER_TOKEN_URL, request, OAuthTokenResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("네이버 토큰 발급 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
    }

    /**
     * [Step 2] 발급받은 토큰으로 유저 정보 조회
     */
    private NaverUserResponse.Profile fetchUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<NaverUserResponse> response = restTemplate.exchange(
                    NAVER_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    NaverUserResponse.class
            );

            NaverUserResponse.Profile profile = response.getBody().response();


            if(profile.email() == null) {
                log.error("네이버 이메일 제공 동의가 필요합니다.");
                throw new BaseException(ResultCode.INVALID_TOKEN);
            }

            return profile;

        } catch (RestClientException e) {
            // 네이버 서버가 터졌거나, 파라미터가 틀렸을 때 (HTTP 에러)
            log.error("네이버 API 통신 에러 (HTTP 상태 코드 문제): {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        } catch (Exception e) {
            // 그 외의 예상치 못한 런타임 에러 (NullPointer 등)
            log.error("네이버 토큰 처리 중 알 수 없는 서버 에러 발생: ", e); // e를 넘겨서 스택 트레이스 전체 출력
            throw new BaseException(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    private record NaverUserResponse(
            @JsonProperty("response") Profile response
    ) {
        public record Profile(String id, String email, String name) {}
    }
}