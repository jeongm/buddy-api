package com.buddy.buddyapi.domain.chat.dto;

import java.util.List;

public record ChatHistoryResponse(
        Long sessionId,
        Long characterId,
        List<ChatMessageDto> messages
) {
    public static ChatHistoryResponse of(Long sessionId, Long characterId, List<ChatMessageDto> messages) {
        return new ChatHistoryResponse(sessionId, characterId, messages);
    }
}
