package com.buddy.buddyapi.dto.response;

import java.time.LocalDate;

public record MonthlyDiaryCountResponse(
        LocalDate date,
        Long count
) {
}
