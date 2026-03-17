package com.buddy.buddyapi.domain.chat.dto;

import com.buddy.buddyapi.domain.chat.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageDto(
        Long messageId,
        String role,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageDto from(ChatMessage message) {
        return new ChatMessageDto(
                message.getMessageId(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
