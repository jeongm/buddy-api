package com.buddy.buddyapi.global.common;

import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private String code;
    private String message;
    private T result;

    // 1. 단순 성공 응답 (데이터 포함)
    public static <T> ApiResponse<T> ok(T result) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), result);
    }

    // 2. 성공 응답 (데이터 + 커스텀 메시지)
    public static <T> ApiResponse<T> ok(String message, T result) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), message, result);
    }

    // 3. 성공 응답 (데이터 없음 - 주로 삭제나 단순 성공 알림)
    public static <T> ApiResponse<Void> ok() {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    // 4. 에러 응답 (기본)
    public static <T> ApiResponse<T> error(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    // 5. 에러 응답 (상세 정보 포함 - Validation 에러 등 전달 시 유용)
    public static <T> ApiResponse<T> error(ResultCode resultCode, T errorDetail) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), errorDetail);
    }

    // 6. 실패 응답 (ResultCode 기반)
    public static <T> ApiResponse<T> fail(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    // 7. 실패 응답 (메시지 커스텀 - 예외 발생 시 상세 메시지 전달용)
    public static <T> ApiResponse<T> fail(ResultCode resultCode, String message) {
        return new ApiResponse<>(resultCode.getCode(), message, null);
    }

    // 8. 실패 응답 (데이터 포함 - 실패 시에도 특정 데이터(예: 남은 인증 횟수 등)를 넘겨야 할 때)
    public static <T> ApiResponse<T> fail(ResultCode resultCode, T result) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), result);
    }

}
