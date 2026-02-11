package com.buddy.buddyapi.global.config;

import com.buddy.buddyapi.entity.RefreshToken;
import com.buddy.buddyapi.repository.RefreshTokenRepository;
import com.buddy.buddyapi.service.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity,
            CustomUserDetailsService userDetailsService,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
        this.userDetailsService = userDetailsService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Access Token 생성
    public String createAccessToken(Long memberSeq) {
        return createToken(memberSeq, accessTokenValidity);
    }

    // Refresh Token 생성
    public String createRefreshToken(Long memberSeq) {
        String token = createToken(memberSeq, refreshTokenValidity);

        // Redis에 저장
        RefreshToken refreshToken = RefreshToken.builder()
                .memberSeq(memberSeq)
                .refreshToken(token)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private String createToken(Long memberSeq, long validity) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setSubject(String.valueOf(memberSeq))
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

        // 이전에 구현한 UserServiceImpl의 loadUserByUsername을 호츌하여 유저 정보를 가져옵니다.
        // 여기서는 간단하게 memberSeq(Subject)만 추출하여 사용하거나
        // UserDetailsService를 주입받아 사용하도록 구성할 수 있습니다.
        String memberSeqStr = claims.getSubject();
        UserDetails userDetails = userDetailsService.loadUserByMemberSeq(Long.parseLong(memberSeqStr));
        // UserDetailsService를 주입받아 처리

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
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
