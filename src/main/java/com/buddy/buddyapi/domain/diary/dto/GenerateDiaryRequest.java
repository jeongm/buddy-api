package com.buddy.buddyapi.domain.diary.dto;

import jakarta.validation.constraints.NotNull;

public record GenerateDiaryRequest(
        @NotNull(message = "세션 ID는 필수입니다.")
        Long sessionId
) {
}
