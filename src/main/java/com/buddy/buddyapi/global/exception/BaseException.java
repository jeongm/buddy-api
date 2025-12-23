package com.buddy.buddyapi.global.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException{
    private final ResultCode resultCode;

    public BaseException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }
}
