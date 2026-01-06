package com.buddy.buddyapi.dto.response;

import java.util.List;

public record OpenAiResponse(
        List<Choice> choices
) {
    // 내부 레코드로 정의하되, Jackson이 인식하기 좋게 분리
    public record Choice(
            Message message,
            String finish_reason,
            Integer index
    ) {}

    public record Message(
            String role,
            String content
    ) {}
}