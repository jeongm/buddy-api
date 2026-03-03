package com.buddy.buddyapi.domain.chat.dto;

import java.util.List;

public record ChatHistoryResponse(
        Long sessionSeq,
        Long characterSeq,
        List<ChatMessageDto> messages
) {
    public static ChatHistoryResponse of(Long sessionSeq, Long characterSeq, List<ChatMessageDto> messages) {
        return new ChatHistoryResponse(sessionSeq, characterSeq, messages);
    }
}
