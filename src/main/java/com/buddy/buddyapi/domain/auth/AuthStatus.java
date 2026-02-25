package com.buddy.buddyapi.domain.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthStatus {
    SUCCESS("성공"),
    REQUIRES_LINKING("소셜 계정 연동 필요"),
    REQUIRES_CHARACTER("캐릭터 생성 필요");

    private final String description;
}
