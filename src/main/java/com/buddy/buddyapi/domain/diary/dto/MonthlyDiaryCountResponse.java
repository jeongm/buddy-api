package com.buddy.buddyapi.domain.diary.dto;

import java.time.LocalDate;

public record MonthlyDiaryCountResponse(
        LocalDate diaryDate,
        Long count
) {
}
