package com.buddy.buddyapi.domain.chat.dto;

public record ChatSendResponse(
        Long sessionId,
        ChatMessageResponse message
) {
    public static ChatSendResponse of(Long sessionId, ChatMessageResponse message) {
        return new ChatSendResponse(
                sessionId,
                message
        );
    }
}
