package com.buddy.buddyapi.domain.auth.component;

import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverTokenVerifier {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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
        String accessToken = getAccessToken(code);
        return getUserInfo(accessToken);
    }

    /**
     * [Step 1] 인가 코드로 네이버 액세스 토큰 발급 (신규 추가)
     */
    private String getAccessToken(String code) {
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
            ResponseEntity<String> response = restTemplate.postForEntity(NAVER_TOKEN_URL, request, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("access_token").asText();
        } catch (Exception e) {
            log.error("네이버 토큰 발급 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
    }

    /**
     * [Step 2] 발급받은 토큰으로 유저 정보 조회
     */
    private OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    NAVER_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode responseNode = rootNode.path("response");

            String oauthId = responseNode.path("id").asText(null);
            String email = responseNode.path("email").asText(null);
            String name = responseNode.path("name").asText(email);

            if(email == null) {
                log.error("네이버 이메일 제공 동의가 필요합니다.");
                throw new BaseException(ResultCode.INVALID_TOKEN);
            }

            return new OAuthUserInfo(email, name, oauthId);

        } catch (Exception e) {
            log.error("네이버 토큰 검증(유저정보 조회) 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
    }
}