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

        // 임시 고유 키 생성
        String tempKey = UUID.randomUUID().toString();

        // Redis에 저장 (JSON 형태로 저장하거나 단순 구분자 사용, TTL 5분)
        // TODO 실제로는 ObjectMapper를 써서 JSON 객체로 넣는 걸 추천하지만, 간단하게 구분자로 넣을 수도 있습니다.
        String redisValue = String.valueOf(memberSeq);
        redisTemplate.opsForValue().set("OAUTH_SUCCESS:" + tempKey, redisValue, Duration.ofMinutes(5));

        String targetUrl = UriComponentsBuilder.fromUriString("https://buddydiary.vercel.app/auth/callback")
                .queryParam("mode","success")
                .queryParam("key", tempKey)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request,response,targetUrl);

    }
}
