package com.buddy.buddyapi.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 커스텀 예외의 최상위 클래스.
 * ResultCode 기반으로 HTTP 응답 상태와 에러 코드를 통합 관리한다.
 */
@Getter
public class BaseException extends RuntimeException {

    private final ResultCode resultCode;
    private final String customMessage;

    /**
     * ResultCode의 기본 메시지를 사용하는 생성자.
     *
     * @param resultCode 에러 코드
     */
    public BaseException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.customMessage = null;
    }

    /**
     * 동적 메시지를 포함하는 생성자.
     * ex) new BaseException(ResultCode.USER_NOT_FOUND, "userId: " + userId + " 를 찾을 수 없습니다.")
     *
     * @param resultCode    에러 코드
     * @param customMessage 동적으로 구성한 상세 메시지
     */
    public BaseException(ResultCode resultCode, String customMessage) {
        super(customMessage);
        this.resultCode = resultCode;
        this.customMessage = customMessage;
    }

    /**
     * 실제 응답에 사용할 메시지 반환. customMessage 우선.
     *
     * @return 응답 메시지
     */
    public String getResponseMessage() {
        return customMessage != null ? customMessage : resultCode.getMessage();
    }
}
