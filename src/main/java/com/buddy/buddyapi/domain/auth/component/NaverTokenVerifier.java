package com.buddy.buddyapi.domain.auth.component;

import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverTokenVerifier {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    /**
     * 프론트에서 넘어온 id_token을 검증하고, 유저 정보(이메일, 이름)를 반환
     */
    public OAuthUserInfo verify(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        // 토큰 까보기
        try{
            // 카카오 서버로 사용자 요청 정보
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
            log.error("네이버 토큰 검증 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
    }
}
