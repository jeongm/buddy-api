package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.dto.response.MonthlyDiaryCountResponse;
import com.buddy.buddyapi.entity.Diary;
import com.buddy.buddyapi.entity.Member;

import java.time.LocalDate;
import java.util.List;

public interface DiaryRepositoryCustom {
    // 1. 월별 일기 개수 조회 (Querydsl로 구현 예정)
    List<MonthlyDiaryCountResponse> findAllMonthlyCount(Long memberSeq, LocalDate startDate, LocalDate endDate);

    // 2. 특정 날짜의 일기 상세 조회 (Querydsl로 구현 예정)
    List<Diary> findAllByMemberAndDiaryDate(Long memberSeq, LocalDate date);

}
