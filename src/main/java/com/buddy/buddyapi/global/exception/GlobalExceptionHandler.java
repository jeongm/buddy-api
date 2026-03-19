package com.buddy.buddyapi.global.exception;


import com.buddy.buddyapi.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 애플리케이션 전역 예외 처리 핸들러.
 * {@link RestControllerAdvice}를 통해 모든 컨트롤러에서 발생하는 예외를 중앙에서 처리하며,
 * 예외 종류에 따라 적절한 HTTP 상태 코드와 {@link ResultCode}를 포함한 {@link ApiResponse}를 반환한다.
 * 핸들러 우선순위: 구체적인 예외 → {@link BaseException} → {@link Exception} (Fallback)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * JWT 토큰이 만료된 경우 발생하는 예외 처리.
     * 액세스 토큰의 유효 기간이 지났을 때 발생하며, 클라이언트에게 재로그인 또는 토큰 갱신을 안내한다.
     *
     * @param e ExpiredJwtException
     * @return 401 Unauthorized {@link ResultCode#EXPIRED_TOKEN}
     */
    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(io.jsonwebtoken.ExpiredJwtException e) {
        log.warn("JWT Token Expired: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ResultCode.EXPIRED_TOKEN, "토큰이 만료되었습니다. 다시 로그인해주세요."));
    }

    /**
     * JWT 토큰의 서명이 올바르지 않을 때 발생하는 예외 처리.
     * 서버의 서명 키와 토큰의 서명이 일치하지 않는 경우 발생하며, 토큰 위변조 가능성이 있다.
     *
     * @param e SignatureException
     * @return 401 Unauthorized {@link ResultCode#TOKEN_SIGNATURE_ERROR}
     */
    @ExceptionHandler(io.jsonwebtoken.security.SignatureException.class)
    public ResponseEntity<ApiResponse<Void>> handleSignatureException(io.jsonwebtoken.security.SignatureException e) {
        log.warn("JWT Signature Error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ResultCode.TOKEN_SIGNATURE_ERROR));
    }

    /**
     * JWT 토큰의 구조가 올바르지 않을 때 발생하는 예외 처리.
     * Header.Payload.Signature 형식을 갖추지 않은 손상된 토큰이 전달된 경우 발생한다.
     *
     * @param e MalformedJwtException
     * @return 401 Unauthorized {@link ResultCode#INVALID_TOKEN}
     */
    @ExceptionHandler(io.jsonwebtoken.MalformedJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJwtException(io.jsonwebtoken.MalformedJwtException e) {
        log.warn("JWT Malformed Error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ResultCode.INVALID_TOKEN));
    }

    /**
     * 서버가 지원하지 않는 형식의 JWT 토큰이 전달될 때 발생하는 예외 처리.
     * 예: 서버가 HS256만 지원하는데 RS256으로 서명된 토큰이 전달된 경우.
     *
     * @param e UnsupportedJwtException
     * @return 401 Unauthorized {@link ResultCode#UNSUPPORTED_TOKEN}
     */
    @ExceptionHandler(io.jsonwebtoken.UnsupportedJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedJwtException(io.jsonwebtoken.UnsupportedJwtException e) {
        log.warn("JWT Unsupported Error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ResultCode.UNSUPPORTED_TOKEN));
    }

    /**
     * Spring Security 인가 실패 시 발생하는 예외 처리. (403 Forbidden)
     * 인증은 완료됐지만 해당 리소스에 대한 접근 권한이 없는 경우 발생한다.
     * 예: 일반 사용자가 관리자 전용 API를 호출하는 경우.
     *
     * @param e AccessDeniedException
     * @return 403 Forbidden {@link ResultCode#FORBIDDEN}
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException e) {
        log.warn("Access Denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ResultCode.FORBIDDEN));
    }

    /**
     * {@code @RequestBody} DTO의 {@code @Valid} 유효성 검사 실패 시 발생하는 예외 처리.
     * 필드별 오류 메시지를 콤마로 연결하여 클라이언트에게 반환한다.
     * 예: {@code @NotBlank}, {@code @Size}, {@code @Email} 등의 제약조건 위반.
     *
     * @param e MethodArgumentNotValidException
     * @return 400 Bad Request {@link ResultCode#INVALID_INPUT} (필드 오류 메시지 포함)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation Exception: {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ResultCode.INVALID_INPUT, errorMessage));
    }

    /**
     * 요청 Body의 JSON 파싱이 실패할 때 발생하는 예외 처리.
     * 잘못된 JSON 형식이거나, Enum 타입에 없는 값이 전달된 경우 발생한다.
     * 예: {@code {"status": "INVALID_VALUE"}} — Enum 변환 실패.
     *
     * @param e HttpMessageNotReadableException
     * @return 400 Bad Request {@link ResultCode#INVALID_INPUT}
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("JSON Parse Exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ResultCode.INVALID_INPUT, "요청 데이터 형식이 올바르지 않습니다."));
    }

    /**
     * URL 경로 변수 또는 쿼리 파라미터의 타입 변환 실패 시 발생하는 예외 처리.
     * 예: {@code Long} 타입 파라미터에 문자열 {@code "abc"}가 전달된 경우.
     *
     * @param e MethodArgumentTypeMismatchException
     * @return 400 Bad Request {@link ResultCode#INVALID_INPUT}
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type Mismatch Exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ResultCode.INVALID_INPUT, "파라미터의 타입이 올바르지 않습니다."));
    }

    /**
     * 지원하지 않는 HTTP 메서드로 요청이 들어올 때 발생하는 예외 처리.
     * 예: {@code POST}만 허용된 엔드포인트에 {@code GET} 요청이 들어온 경우.
     *
     * @param e HttpRequestMethodNotSupportedException
     * @return 405 Method Not Allowed {@link ResultCode#METHOD_NOT_ALLOWED}
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method Not Supported: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ResultCode.METHOD_NOT_ALLOWED));
    }

    /**
     * {@code @PathVariable}, {@code @RequestParam}에 선언된 {@code @Validated} 유효성 검사 실패 시 발생하는 예외 처리.
     * 필드명과 오류 메시지를 함께 반환하여 클라이언트가 어떤 파라미터가 잘못됐는지 파악할 수 있도록 한다.
     * 예: {@code GET /api/v1/diary?page=-1} — page는 양수여야 한다는 제약 위반.
     *
     * @param e ConstraintViolationException
     * @return 400 Bad Request {@link ResultCode#INVALID_INPUT} (제약 위반 필드 및 메시지 포함)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Constraint Violation Exception: {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ResultCode.INVALID_INPUT, errorMessage));
    }

    /**
     * 비즈니스 로직에서 명시적으로 발생시키는 커스텀 예외 처리.
     * {@link BaseException}을 상속한 모든 예외를 처리하며, {@link ResultCode}에 정의된 HTTP 상태와 코드로 응답한다.
     * customMessage가 있으면 해당 메시지를, 없으면 ResultCode의 기본 메시지를 반환한다.
     *
     * @param e BaseException (또는 하위 커스텀 예외)
     * @return ResultCode에 정의된 HTTP Status와 에러 코드 및 메시지
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        ResultCode rc = e.getResultCode();
        log.warn("BaseException [{}]: {}", rc.getCode(), e.getResponseMessage());
        return ResponseEntity.status(rc.getHttpStatus())
                .body(ApiResponse.fail(rc, e.getResponseMessage()));
    }

    /**
     * DB 제약조건(Unique, Not Null, Foreign Key 등) 위반 시 발생하는 예외 처리.
     * JPA/Hibernate가 DB에 데이터를 저장하는 과정에서 제약 조건에 위배될 때 발생.
     * 예: Unique 컬럼에 중복된 값 삽입, Not Null 컬럼에 null 삽입, 외래키 참조 무결성 위반.
     *
     * @param e DataIntegrityViolationException
     * @return 409 Conflict {@link ResultCode#DATA_INTEGRITY_VIOLATION}
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("Data Integrity Violation: ", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ResultCode.DATA_INTEGRITY_VIOLATION, "데이터 제약 조건 위반이 발생했습니다."));
    }

    /**
     * 위의 핸들러에서 처리되지 않은 모든 예외에 대한 Fallback 처리.
     * 예상치 못한 서버 에러를 클라이언트에게 500으로 응답하고, 서버 로그에 전체 스택 트레이스를 기록한다.
     * 이 핸들러가 호출되면 운영 환경에서 즉각적인 모니터링 및 대응이 필요하다.
     *
     * @param e Exception
     * @return 500 Internal Server Error {@link ResultCode#INTERNAL_SERVER_ERROR}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected Exception: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ResultCode.INTERNAL_SERVER_ERROR));
    }
}
