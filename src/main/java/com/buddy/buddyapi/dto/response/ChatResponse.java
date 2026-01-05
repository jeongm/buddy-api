package com.buddy.buddyapi.dto.response;

import com.buddy.buddyapi.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatResponse(
        Long sessionId,
        Long messageSeq,
        String role,
        String content,
        LocalDateTime createdAt
) {
    public static ChatResponse from(ChatMessage message, Long sessionId) {
        return new ChatResponse(
                sessionId,
                message.getMessageSeq(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
