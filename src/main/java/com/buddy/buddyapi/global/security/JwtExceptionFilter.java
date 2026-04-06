package com.buddy.buddyapi.global.security;

import com.buddy.buddyapi.global.common.ApiResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 필터 체인(JwtAuthenticationFilter)에서 발생하는 예외를 처리하는 필터.
 * ControllerAdvice(@RestControllerAdvice)는 필터 단의 예외를 잡지 못하므로,
 * 예외가 발생하면 이 필터에서 catch하여 직접 ApiResponse 규격으로 응답을 내려보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 다음 필터(JwtAuthenticationFilter 등)로 요청 전달
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token Expired: {}", e.getMessage());
            setErrorResponse(response, ResultCode.EXPIRED_TOKEN, "토큰이 만료되었습니다. 다시 로그인해주세요.");
        } catch (SignatureException e) {
            log.warn("JWT Signature Error: {}", e.getMessage());
            setErrorResponse(response, ResultCode.TOKEN_SIGNATURE_ERROR, ResultCode.TOKEN_SIGNATURE_ERROR.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT Malformed Error: {}", e.getMessage());
            setErrorResponse(response, ResultCode.INVALID_TOKEN, ResultCode.INVALID_TOKEN.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT Unsupported Error: {}", e.getMessage());
            setErrorResponse(response, ResultCode.UNSUPPORTED_TOKEN, ResultCode.UNSUPPORTED_TOKEN.getMessage());
        } catch (Exception e) {
            // 위에서 잡히지 않은 예상치 못한 에러
            log.error("JWT Filter Exception: ", e);
            setErrorResponse(response, ResultCode.INTERNAL_SERVER_ERROR, "인증 처리 중 서버 에러가 발생했습니다.");
        }
    }

    /**
     * 필터 단에서 터진 예외를 공통 응답 포맷(ApiResponse) 형태의 JSON으로 클라이언트에 반환한다.
     */
    private void setErrorResponse(HttpServletResponse response, ResultCode resultCode, String customMessage) throws IOException {
        // 1. 응답 상태 코드 및 헤더 설정
        response.setStatus(resultCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8"); // 한글 깨짐 방지

        // 2. 공통 에러 응답 객체 생성
        ApiResponse<Void> apiResponse = ApiResponse.error(resultCode, customMessage);

        // 3. ObjectMapper를 통해 JSON 문자열로 변환하여 출력
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
