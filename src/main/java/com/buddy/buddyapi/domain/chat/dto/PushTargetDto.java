package com.buddy.buddyapi.domain.chat.dto;

public record PushTargetDto(
        Long sessionId,
        String pushToken
) {
}
