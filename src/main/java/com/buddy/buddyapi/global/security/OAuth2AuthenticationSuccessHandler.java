package com.buddy.buddyapi.global.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long memberSeq = userDetails.memberSeq();

        String tempKey = UUID.randomUUID().toString();

        String redisValue = String.valueOf(memberSeq);
        redisTemplate.opsForValue().set("OAUTH_SUCCESS:" + tempKey, redisValue, Duration.ofMinutes(5));

        String targetUrl = UriComponentsBuilder.fromUriString("https://buddydiary.vercel.app/auth/callback")
                .queryParam("mode","success")
                .queryParam("key", tempKey)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request,response,targetUrl);

    }
}
