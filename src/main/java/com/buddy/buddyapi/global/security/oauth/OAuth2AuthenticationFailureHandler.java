package com.buddy.buddyapi.global.security.oauth;

import com.buddy.buddyapi.global.exception.ResultCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler  extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        String errorMessage = extractErrorCode(exception);

        if (errorMessage != null && errorMessage.startsWith(ResultCode.ALREADY_SIGNED_UP_EMAIL.name())) {
            String[] parts = errorMessage.split(":");
            String email = parts[1];
            String provider = parts[2];
            String oauthId = parts[3];


            // TODO queryparam은 보안상 좋지 않음 레디스 등으로 수정해야함
            // 프론트에서 리다이렉트 url을 보고 연동 요청을 보내야하는건가
            String targetUrl = UriComponentsBuilder.fromUriString("http://buddydiary.vercel.app/auth/callback")
                    .queryParam("mode", "link")
                    .queryParam("email", email)
                    .queryParam("provider", provider)
                    .queryParam("oauthId", oauthId)
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
