package com.buddy.buddyapi.global.security;

import com.buddy.buddyapi.global.exception.ResultCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler  extends SimpleUrlAuthenticationFailureHandler {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        String errorMessage = extractErrorCode(exception);

        if (errorMessage != null && errorMessage.startsWith(ResultCode.ALREADY_SIGNED_UP_EMAIL.name())) {
            String[] parts = errorMessage.split(":");
            String email = parts[1];
            String provider = parts[2];
            String oauthId = parts[3];

            // 보안용 임시 키 발급
            String tempKey = UUID.randomUUID().toString();

            // Redis에 저장 (예: "link:tempKey" -> "email:provider:oauthId", TTL 5분)
            String redisValue = email + ":" + provider + ":" + oauthId;
            redisTemplate.opsForValue().set("OAUTH_LINK:" + tempKey, redisValue, Duration.ofMinutes(5));

            // 프론트에서 리다이렉트 url을 보고 연동 요청을 보내도록
            String targetUrl = UriComponentsBuilder.fromUriString("https://buddydiary.vercel.app/auth/callback")
                    .queryParam("mode", "link")
                    .queryParam("email", email)
                    .queryParam("provider",provider)
                    .queryParam("key", tempKey)
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request,response,targetUrl);
        } else {
            super.onAuthenticationFailure(request, response, exception);
        }


    }

    private String extractErrorCode(AuthenticationException exception) {

        if (exception instanceof OAuth2AuthenticationException oAuth2Ex) {
            return oAuth2Ex.getError().getErrorCode();
        }

        return exception.getMessage();
    }
}
