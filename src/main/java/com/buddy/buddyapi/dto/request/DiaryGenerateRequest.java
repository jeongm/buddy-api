package com.buddy.buddyapi.dto.request;

import jakarta.validation.constraints.NotNull;

public record DiaryGenerateRequest(
        @NotNull(message = "세션 ID는 필수입니다.")
        Long sessionId
) {
}
