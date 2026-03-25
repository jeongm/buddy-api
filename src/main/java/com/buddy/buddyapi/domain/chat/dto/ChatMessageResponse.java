package com.buddy.buddyapi.domain.chat.dto;

import com.buddy.buddyapi.domain.chat.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        String role,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getMessageId(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
