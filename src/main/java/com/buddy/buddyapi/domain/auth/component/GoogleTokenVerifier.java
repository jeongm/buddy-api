package com.buddy.buddyapi.domain.auth.component;

import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleTokenVerifier(@Value("${google.audiences}") List<String> audiences) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(audiences)
                .build();
    }

    /**
     * 프론트에서 넘어온 id_token을 검증하고, 유저 정보(이메일, 이름)를 반환
     */
    public OAuthUserInfo verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                log.error("🚨 구글 토큰 검증 실패: 토큰이 만료되었거나 Audience 불일치");
                throw new BaseException(ResultCode.INVALID_TOKEN);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String oauthId = payload.getSubject();

            return new OAuthUserInfo(email, name, oauthId,null,null);

        } catch (GeneralSecurityException | IOException e) {
            log.error("🚨 구글 토큰 파싱 에러: {}", e.getMessage());
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }

    }


}
