package com.buddy.buddyapi.domain.chat.dto;

public record ChatSendResponse(
        Long sessionSeq,
        ChatMessageDto message
) {
    public static ChatSendResponse of(Long sessionSeq, ChatMessageDto message) {
        return new ChatSendResponse(
                sessionSeq,
                message
        );
    }
}
