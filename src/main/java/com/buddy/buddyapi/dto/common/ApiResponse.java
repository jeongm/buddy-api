package com.buddy.buddyapi.dto.common;

import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private String code;
    private String message;
    private T result;

    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), result);
    }

    // 성공 시 커스텀 메시지가 필요한 경우
    public static <T> ApiResponse<T> success(String message, T result) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), message, result);
    }

    // 에러 발생 시 호출 (Enum을 인자로 받음)
    public static <T> ApiResponse<T> error(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), null);
    }


}
