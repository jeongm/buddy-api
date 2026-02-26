package com.buddy.buddyapi.domain.chat.dto;

import com.buddy.buddyapi.domain.chat.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageDto(
        Long messageSeq,
        String role,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageDto from(ChatMessage message) {
        return new ChatMessageDto(
                message.getMessageSeq(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
