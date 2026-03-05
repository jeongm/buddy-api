package com.buddy.buddyapi.domain.member.dto;

import com.buddy.buddyapi.domain.member.MemberInsight;
import lombok.Builder;

@Builder
public record WeeklyInsightResponse(
        String weeklyIdentity,
        String weeklyKeyword
) {
    public static WeeklyInsightResponse from(MemberInsight insight) {
        if (insight == null) {
            return WeeklyInsightResponse.builder()
                    .weeklyIdentity(null)
                    .weeklyKeyword(null)
                    .build();
        }

        return WeeklyInsightResponse.builder()
                .weeklyIdentity(insight.getWeeklyIdentity())
                .weeklyKeyword(insight.getWeeklyKeyword())
                .build();
    }
}
