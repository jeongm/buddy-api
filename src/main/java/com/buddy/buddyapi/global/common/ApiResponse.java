package com.buddy.buddyapi.global.common;

import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final String code;
    private final String message;
    private final T result;

    // 1. 단순 성공 응답 (데이터 포함)
    public static <T> ApiResponse<T> ok(T result) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), result);
    }

    // 2. 성공 응답 (데이터 + 커스텀 메시지)
    public static <T> ApiResponse<T> ok(String message, T result) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), message, result);
    }

    // 3. 성공 응답 (데이터 없음 - 주로 삭제나 단순 성공 알림)
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    // 4. 에러 응답 (기본)
    public static <T> ApiResponse<T> error(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    // 5. 에러 응답 (상세 정보 포함 - 예외 발생 시 상세 메시지 전달용)
    public static <T> ApiResponse<T> error(ResultCode resultCode, String message) {
        return new ApiResponse<>(resultCode.getCode(), message, null);
    }


    // 6. 실패 응답 (데이터 포함 - 실패 시 특정 데이터나 Validation 에러 목록 등을 넘겨야 할 때)
    public static <T> ApiResponse<T> error(ResultCode resultCode, T result) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), result);
    }

    // 7. 실패 응답 (메시지 커스텀 + 데이터 포함)
    public static <T> ApiResponse<T> error(ResultCode resultCode, String message, T result) {
        return new ApiResponse<>(resultCode.getCode(), message, result);
    }

}
