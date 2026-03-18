package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.domain.diary.dto.MonthlyDiaryCountResponse;
import com.buddy.buddyapi.domain.diary.dto.TagResponse;
import com.buddy.buddyapi.domain.insight.dto.TagNameCountResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepositoryCustom {
    // 월별 일기 개수 조회
    List<MonthlyDiaryCountResponse> findAllMonthlyCount(Long memberId, LocalDate startDate, LocalDate endDate);

    // 특정 날짜의 일기 상세 조회
    List<Diary> findAllByMemberAndDiaryDate(Long memberId, LocalDate date);

    // 일기 상세페이지 - 모든 태그정보, 대화 로그
    Optional<Diary> findDetailByDiaryIdAndMemberId(Long diaryId, Long memberId);

    Slice<Diary> searchMyDiaries(Long memberId, String search, Pageable pageable);

    List<TagResponse> findRecentTopTags(Long memberId);

    List<String> findDiaryContentsByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate);

    List<TagNameCountResponse> findTopTagsByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate, int limit);
}
