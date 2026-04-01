package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.domain.insight.dto.TagNameCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


/**
 * 해당 클래스는 조회전용 클래스로 비지니스로직이 포함되지않습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryQueryService {
    private final DiaryRepository diaryRepository;

    // 스트릭 재계산용 날짜 조회
    public List<LocalDate> getDistinctDiaryDates(Long memberId) {
        return diaryRepository.findDistinctDiaryDatesDescByMemberId(memberId);
    }

    // AI 주간 칭호용 저번주 일기 내용 조회
    public List<String> getDiaryContentsByDateRange(Long memberId, LocalDate startDate, LocalDate endDate) {
        return diaryRepository.findDiaryContentsByMemberAndDateRange(memberId, startDate, endDate);
    }

    // 주간 리포트용 Top 5 태그 조회
    public List<TagNameCountResponse> getTopTagsByDateRange(Long memberId, LocalDate startDate, LocalDate endDate, int limit) {
        return diaryRepository.findTopTagsByMemberAndDateRange(memberId, startDate, endDate, limit);
    }
}
