package com.buddy.buddyapi.dto.common;

import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// TODO APIRESPONSE 이걸로 수정하기
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 외부에서 생성자 직접 호출 방지
public class ApiResponse2<T> {
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
}