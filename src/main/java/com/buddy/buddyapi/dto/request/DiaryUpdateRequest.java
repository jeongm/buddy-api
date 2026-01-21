package com.buddy.buddyapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

public record DiaryUpdateRequest(
        String title,
        @NotBlank(message = "일기 내용은 필수입니다.")
        String content,
        @NotNull(message = "일기 날짜를 입력해주세요")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate diaryDate,
        List<String> tags
) {
}
