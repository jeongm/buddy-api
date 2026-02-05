package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.dto.response.MonthlyDiaryCountResponse;
import com.buddy.buddyapi.entity.Diary;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.buddy.buddyapi.entity.QDiary.diary;
import static com.buddy.buddyapi.entity.QDiaryTag.diaryTag;
import static com.buddy.buddyapi.entity.QTag.tag;
import static com.buddy.buddyapi.entity.QChatSession.chatSession;

@RequiredArgsConstructor
public class DiaryRepositoryImpl implements DiaryRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    /**
     * 월별 일기 작성 일자 별 개수 조회 (캘린더 잔디용)
     */
    @Override
    public List<MonthlyDiaryCountResponse> findAllMonthlyCount(Long memberSeq, LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .select(Projections.constructor(MonthlyDiaryCountResponse.class,
                        diary.diaryDate,
                        diary.count()
                ))
                .from(diary)
                .where(
                        diary.member.memberSeq.eq(memberSeq),
                        diary.diaryDate.between(startDate, endDate)
                )
                .groupBy(diary.diaryDate)
                .fetch();
    }

    /**
     * 특정 날짜의 일기 상세 조회 (태그까지 Fetch Join)
     */
    @Override
    public List<Diary> findAllByMemberAndDiaryDate(Long memberSeq, LocalDate date) {
        return queryFactory
                .selectFrom(diary)
                .distinct()
                .leftJoin(diary.diaryTags, diaryTag).fetchJoin()
                .leftJoin(diaryTag.tag, tag).fetchJoin()
                .where(
                        diary.member.memberSeq.eq(memberSeq),
                        diary.diaryDate.eq(date)
                )
                .orderBy(diary.createdAt.desc())
                .fetch();

    }

    /**
     * 일기 상세 조회 - ai작성시 chatSession도 함께 넘겨줌
     */
    @Override
    public Optional<Diary> findDetailByDiarySeqAndMemberSeq(Long diarySeq, Long memberSeq) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(diary)
                        .distinct()
                        .leftJoin(diary.chatSession).fetchJoin()
                        .leftJoin(diary.diaryTags, diaryTag).fetchJoin()
                        .leftJoin(diaryTag.tag, tag). fetchJoin()
                        .where(diary.diarySeq.eq(diarySeq), diary.member.memberSeq.eq(memberSeq))
                        .fetchOne()
        );
    }

    /**
     * AI 일기 작성 시 대화 기록 가져오기
     */
}
