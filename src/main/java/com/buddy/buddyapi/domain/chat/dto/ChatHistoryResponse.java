package com.buddy.buddyapi.domain.chat.dto;

import java.util.List;

public record ChatHistoryResponse(
        Long sessionId,
        Long characterId,
        List<ChatMessageResponse> messages
) {
    public static ChatHistoryResponse of(Long sessionId, Long characterId, List<ChatMessageResponse> messages) {
        return new ChatHistoryResponse(sessionId, characterId, messages);
    }
}
