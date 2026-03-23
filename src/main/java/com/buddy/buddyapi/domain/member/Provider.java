package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;

public enum Provider {
    KAKAO, GOOGLE, NAVER, APPLE;

    public static Provider from(String registrationId) {
        try {
            return Provider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BaseException(ResultCode.UNSUPPORTED_PROVIDER);
        }
    }
}
