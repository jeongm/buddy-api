package com.buddy.buddyapi.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    // Access Token 생성
    public String createAccessToken(Long memberId) {
        return createToken(memberId, accessTokenValidity);
    }

    // Refresh Token 생성
    public String createRefreshToken(Long memberId) {
        return createToken(memberId, refreshTokenValidity);
    }

    private String createToken(Long memberId, long validity) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 인증 정보 조회
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // 여기서는 간단하게 memberId(Subject)만 추출하여 사용
        Long memberId = Long.parseLong(claims.getSubject());

        return new UsernamePasswordAuthenticationToken(
                memberId,
                null,
                List.of()
        );
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        // 에러 발생 시 호출한 Filter로 예외가 그대로 전달됨
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
            throw e;
        } catch (Exception e) {
            log.info("유효하지 않은 JWT 토큰입니다.");
            return false;
        }

    }

}
