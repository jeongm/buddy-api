package com.buddy.buddyapi.domain.insight.dto;

import com.buddy.buddyapi.domain.insight.MemberInsight;
import lombok.Builder;

@Builder
public record WeeklyIdentityResponse(
        String weeklyIdentity,
        String weeklyKeyword
) {
    public static WeeklyIdentityResponse from(MemberInsight insight) {
        if (insight == null) {
            return WeeklyIdentityResponse.builder()
                    .weeklyIdentity(null)
                    .weeklyKeyword(null)
                    .build();
        }

        return WeeklyIdentityResponse.builder()
                .weeklyIdentity(insight.getWeeklyIdentity())
                .weeklyKeyword(insight.getWeeklyKeyword())
                .build();
    }
}
