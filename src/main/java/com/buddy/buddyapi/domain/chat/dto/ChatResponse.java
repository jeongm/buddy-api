package com.buddy.buddyapi.domain.chat.dto;

import com.buddy.buddyapi.domain.chat.ChatMessage;

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
