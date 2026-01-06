package com.buddy.buddyapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DiaryGenerateRequest(
        @NotBlank(message = "세션 ID는 필수입니다.")
        Long sessionId
) {
}
