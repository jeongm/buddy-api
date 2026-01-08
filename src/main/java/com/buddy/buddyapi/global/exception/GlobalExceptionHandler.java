package com.buddy.buddyapi.global.exception;


import com.buddy.buddyapi.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 우리가 직접 정의한 BaseException 처리
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        log.warn("BaseException: {}", e.getResultCode().getMessage());
        ResultCode rc = e.getResultCode();
        return ResponseEntity
                .status(rc.getHttpStatus())
                .body(new ApiResponse<>(rc.getCode(), rc.getMessage(), null));
    }

    // 그 외 예상치 못한 시스템 예외 처리 (500 에러 등)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected Exception: ", e);
        return ResponseEntity
                .status(500)
                .body(new ApiResponse<>(ResultCode.INTERNAL_SERVER_ERROR.getCode(), "서버 내부 에러가 발생했습니다.", null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 유효성 검사 에러 메시지 중 첫 번째 것을 가져옴
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        log.warn("Validation Exception: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(ResultCode.INVALID_INPUT.getCode(), errorMessage, null));
    }
}
