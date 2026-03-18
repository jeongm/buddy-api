package com.buddy.buddyapi.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 메시지 요청 객체")
public record ChatRequest(
        Long sessionId, // 처음 보낼 때는 null 가능
        String content
) {
}
