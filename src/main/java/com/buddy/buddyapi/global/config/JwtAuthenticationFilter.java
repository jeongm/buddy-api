package com.buddy.buddyapi.global.config;

import com.buddy.buddyapi.dto.common.ApiResponse;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws IOException, ServletException {

        String path = request.getRequestURI();

        // 1. OPTIONS 메서드는 토큰 검사 없이 통과 (CORS 예비 요청 처리)
        if (request.getMethod().equals("OPTIONS") || path.startsWith("/api/v1/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Request Header에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검사 및 인증 처리
        try {
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.error("만료된 토큰입니다: {}", e.getMessage());
            sendErrorResponse(response, ResultCode.EXPIRED_TOKEN);
        } catch (SignatureException | MalformedJwtException e) {
            log.error("잘못된 토큰 서명입니다: {}", e.getMessage());
            sendErrorResponse(response, ResultCode.TOKEN_SIGNATURE_ERROR);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 토큰 형식입니다: {}", e.getMessage());
            sendErrorResponse(response, ResultCode.UNSUPPORTED_TOKEN);
        } catch (Exception e) {
            log.error("인증 실패: {}", e.getMessage());
            sendErrorResponse(response, ResultCode.INVALID_TOKEN);
        }



    }

    private void sendErrorResponse(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(resultCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow_Origin","*");

        ApiResponse<Void> apiResponse = ApiResponse.error(resultCode);

        //ObjectMapper를 사용해 JSON문자열로 변환
        String result = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(result);
    }

    // Header "Bearer" 부분을 떼고 토큰 값만 가져오는 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
