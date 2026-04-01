package com.buddy.buddyapi.domain.insight.dto;

/**
 * 연속 기록 응답 DTO.
 *
 * @param currentStreak 오늘 기준 현재 연속 기록 일수
 * @param bestStreak    전체 기간 중 최고 연속 기록 일수
 */
public record StreakResponse(
        int currentStreak,
        int bestStreak
) {
}
