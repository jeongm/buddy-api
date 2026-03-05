package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.domain.diary.dto.MonthlyDiaryCountResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.buddy.buddyapi.domain.diary.QDiary.diary;
import static com.buddy.buddyapi.domain.diary.QDiaryTag.diaryTag;
import static com.buddy.buddyapi.domain.diary.QTag.tag;

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
     * 일기 목록 무한 스크롤 및 검색 (Querydsl 동적 쿼리)
     */
    @Override
    public Slice<Diary> searchMyDiaries(Long memberSeq, String search, Pageable pageable) {

        int pageSize = pageable.getPageSize();

        // 1. Querydsl로 데이터 조회 (limit를 기존 사이즈보다 1개 더 가져옵니다 -> Slice 무한 스크롤의 핵심!)
        List<Diary> content = queryFactory
                .selectFrom(diary)
                .distinct() // 태그 때문에 데이터 뻥튀기 방지
                .leftJoin(diary.diaryTags, diaryTag)
                .leftJoin(diaryTag.tag, tag)
                .where(
                        diary.member.memberSeq.eq(memberSeq),
                        searchContains(search) // 👈 마법의 동적 조건 메서드!
                )
                // 컨트롤러에서 설정한 기본 정렬 조건(diaryDate DESC)을 하드코딩으로 고정해 두면 편합니다.
                .orderBy(diary.diaryDate.desc(), diary.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageSize + 1) // 화면에 보여줄 개수 + 1개 (다음 페이지 유무 확인용)
                .fetch();

        // 2. 다음 페이지 여부 확인
        boolean hasNext = false;
        if (content.size() > pageSize) {
            content.remove(pageSize); // 화면에 보여줄 개수(예: 10개)만 남기고 진짜 마지막 1개(11번째)는 뺌
            hasNext = true;
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    /**
     * 원하는 기간동안의 일기를 내용만 조회
     */
    @Override
    public List<String> findDiaryContentsByMemberAndDateRange(Long memberSeq, LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .select(diary.content)
                .from(diary)
                .where(
                        diary.member.memberSeq.eq(memberSeq),
                        diary.diaryDate.between(startDate, endDate)
                )
                .fetch();
    }

    /**
     * 원하는 기간 동안 최다 빈도의 태그 조회 (단건)
     */

    @Override
    public String findTopTagNameByMemberAndDateRange(Long memberSeq, LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .select(tag.name)
                .from(diary)
                .join(diary.diaryTags, diaryTag)
                .join(diaryTag.tag, tag)
                .where(
                        diary.member.memberSeq.eq(memberSeq),
                        diary.diaryDate.between(startDate,endDate)
                )
                .groupBy(tag.tagSeq, tag.name)
                .orderBy(tag.tagSeq.count().desc())
                .limit(1)
                .fetchOne();
    }

    /**
     * 동적 쿼리를 위한 조건 생성 메서드
     * 검색어가 null 이거나 비어있으면 null 을 반환하여 where 절에서 무시됨!
     */
    private BooleanExpression searchContains(String search) {
        if (!StringUtils.hasText(search)) {
            return null; // 검색어 없으면 전체 조회 (조건 무시)
        }
        return diary.title.contains(search)
                .or(diary.content.contains(search))
                .or(tag.name.contains(search));
    }

}
