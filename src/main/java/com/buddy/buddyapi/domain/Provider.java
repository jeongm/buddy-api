package com.buddy.buddyapi.domain;

public enum Provider {
    KAKAO, GOOGLE, NAVER;

    // 문자열을 받아서 알맞은 Enum을 찾아주는 메서드
    public static Provider from(String registrationId) {
        return Provider.valueOf(registrationId.toUpperCase());
    }
}
