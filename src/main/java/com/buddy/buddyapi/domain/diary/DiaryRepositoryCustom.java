package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.domain.diary.dto.MonthlyDiaryCountResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepositoryCustom {
    // 월별 일기 개수 조회
    List<MonthlyDiaryCountResponse> findAllMonthlyCount(Long memberSeq, LocalDate startDate, LocalDate endDate);

    // 특정 날짜의 일기 상세 조회
    List<Diary> findAllByMemberAndDiaryDate(Long memberSeq, LocalDate date);

    // 일기 상세페이지 - 모든 태그정보, 대화 로그
    Optional<Diary> findDetailByDiarySeqAndMemberSeq(Long diarySeq, Long memberSeq);
}
