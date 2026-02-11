package com.buddy.buddyapi.global.exception;


import com.buddy.buddyapi.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 토큰 만료 에러 - 401
    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(io.jsonwebtoken.ExpiredJwtException e) {
        log.warn("JWT Token Expired: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(ResultCode.UNAUTHORIZED.getCode(), "토큰이 만료되었습니다. 다시 로그인해주세요.", null));
    }

    // 잘못된 토큰 에러 (서명 오류, 형식 오류 등) - 401
    @ExceptionHandler({
            io.jsonwebtoken.security.SignatureException.class,
            io.jsonwebtoken.MalformedJwtException.class,
            io.jsonwebtoken.UnsupportedJwtException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidJwtException(Exception e) {
        log.warn("Invalid JWT Token: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(ResultCode.UNAUTHORIZED.getCode(), "유효하지 않은 인증 토큰입니다.", null));
    }

    // @Valid 유효성 검사 실패 - 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 유효성 검사 에러 메시지 중 첫 번째 것을 가져옴
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation Exception: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(ResultCode.INVALID_INPUT.getCode(), errorMessage, null));
    }

    // JSON 파싱 에러 (타입 불일치, 형식 오류, 이중 객체 등) 처리 - 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("JSON Parse Exception: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                        ResultCode.INVALID_INPUT.getCode(),
                        "요청 데이터 형식이 올바르지 않습니다. 필드 타입이나 JSON 구조를 확인해주세요.",
                        null
                ));
    }

    // 파라미터 타입 불일치 ex)GET /api/v1/diary/abc -> Long이 들어와야하는데 문자가 들어옴 -400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Type Mismatch Exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(ResultCode.INVALID_INPUT.getCode(), "파라미터의 타입이 올바르지 않습니다.", null));
    }

    // 지원하지 않는 HTTP 메서드 호출 - 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method Not Supported: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiResponse<>(ResultCode.METHOD_NOT_ALLOWED.getCode(), "지원하지 않는 HTTP 메서드입니다.", null));
    }

    // @RequestBody가 아닌 @PathVariable이나 @RequestParam에 붙인 유효성 검사가 실패할 때 발생 ex)GET /api/v1/diary?page=-1
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Constraint Violation Exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(ResultCode.INVALID_INPUT.getCode(), e.getMessage(), null));
    }

    // 우리가 직접 정의한 BaseException 처리
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        log.warn("BaseException: {}", e.getResultCode().getMessage());
        ResultCode rc = e.getResultCode();
        return ResponseEntity
                .status(rc.getHttpStatus())
                .body(new ApiResponse<>(rc.getCode(), rc.getMessage(), null));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        // 데이터 무결성 위반
        log.error("Data Integrity Violation: ", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(ResultCode.INVALID_REQUEST.getCode(), "데이터 제약 조건 위반이 발생했습니다.", null));
    }

    // 그 외 예상치 못한 시스템 예외 처리 (500 에러 등)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected Exception: ", e);
        return ResponseEntity
                .status(500)
                .body(new ApiResponse<>(ResultCode.INTERNAL_SERVER_ERROR.getCode(), "서버 내부 에러가 발생했습니다.", null));
    }
}
