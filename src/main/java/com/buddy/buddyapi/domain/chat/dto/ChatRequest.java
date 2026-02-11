package com.buddy.buddyapi.domain.chat.dto;

public record ChatRequest(
        Long sessionId, // 처음 보낼 때는 null 가능
        String content
) {
}
