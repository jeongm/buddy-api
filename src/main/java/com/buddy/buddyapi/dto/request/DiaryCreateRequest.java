package com.buddy.buddyapi.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DiaryCreateRequest(
        String title,
        @NotBlank(message = "일기 내용은 필수입니다.")
        String content,
        String imageUrl,
        List<Long> tagSeqs
) {
}
