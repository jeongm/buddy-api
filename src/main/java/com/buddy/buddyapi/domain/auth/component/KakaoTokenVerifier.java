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
public class KakaoTokenVerifier {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    // 프론트가 낚아챌 때 썼던 가짜 주소 (ex. https://buddy.com/oauth/callback)
    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    /**
     * 프론트에서 넘어온 id_token을 검증하고, 유저 정보(이메일, 이름)를 반환
     */
    public OAuthUserInfo verify(String code) {
        // 인가 코드로 카카오 액세스 토큰 쟁취!
        String accessToken = getAccessToken(code);

        // 얻어낸 토큰으로 유저 정보 조회!
        return getUserInfo(accessToken);
    }

    /**
     * 인가 코드로 카카오 액세스 토큰 발급
     */
    private String getAccessToken(String code){
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
            ResponseEntity<String> response = restTemplate.postForEntity(KAKAO_TOKEN_URL, request, String.class);
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("access_token").asText();
        } catch (Exception e) {
            log.error("카카오 토큰 발급 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
    }

    /**
     * 발급받은 토큰으로 유저 정보 조회 (작성자님의 기존 verify 메서드 로직)
     */
    private OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode kakaoAccount = rootNode.path("kakao_account");

            String oauthId = rootNode.path("id").asText();
            String email = kakaoAccount.path("email").asText(null);
            String name = kakaoAccount.path("profile").path("nickname").asText(email);

            if(email == null) {
                log.error("카카오 이메일 제공 동의가 필요합니다.");
                throw new BaseException(ResultCode.INVALID_TOKEN);
            }

            return new OAuthUserInfo(email, name, oauthId);

        } catch (Exception e) {
            log.error("카카오 유저 정보 조회 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
    }


}
