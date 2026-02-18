package com.buddy.buddyapi.global.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberSeq = userDetails.memberSeq();

        String accessToken = jwtTokenProvider.createAccessToken(memberSeq);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberSeq);

        // TODO queryparam으로 보내는건 보안상 최악임 수정해야함
        String targetUrl = UriComponentsBuilder.fromUriString("https://buddydiary.vercel.app/auth/callback")
                .queryParam("mode","success")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                        .build().toUriString();

        getRedirectStrategy().sendRedirect(request,response,targetUrl);

    }
}
