package com.buddy.buddyapi.domain.chat.dto;

public record ChatSendResponse(
        Long sessionId,
        ChatMessageDto message
) {
    public static ChatSendResponse of(Long sessionId, ChatMessageDto message) {
        return new ChatSendResponse(
                sessionId,
                message
        );
    }
}
