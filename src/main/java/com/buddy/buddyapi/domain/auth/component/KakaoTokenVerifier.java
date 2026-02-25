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
public class KakaoTokenVerifier {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    /**
     * 프론트에서 넘어온 id_token을 검증하고, 유저 정보(이메일, 이름)를 반환
     */
    public OAuthUserInfo verify(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        // 토큰 까보기
        try{
            // 카카오 서버로 사용자 요청 정보
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

            return new OAuthUserInfo(email, name,oauthId);

        } catch (Exception e) {
            log.error("카카오 토큰 검증 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }
    }
}
