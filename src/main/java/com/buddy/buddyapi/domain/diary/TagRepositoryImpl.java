package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.domain.diary.dto.TagResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import static com.buddy.buddyapi.domain.diary.QDiary.diary;
import static com.buddy.buddyapi.domain.diary.QDiaryTag.diaryTag;
import static com.buddy.buddyapi.domain.diary.QTag.tag;

@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TagResponse> findRecentTopTags(Long memberSeq) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        return queryFactory
                .select(Projections.constructor(TagResponse.class,
                        tag.tagSeq,
                        tag.name))
                .from(diaryTag)
                .join(diaryTag.diary, diary)
                .join(diaryTag.tag, tag)
                .where(
                        diary.member.memberSeq.eq(memberSeq),
                        diary.diaryDate.goe(thirtyDaysAgo)
                )
                .groupBy(tag.tagSeq, tag.name)
                .orderBy(diaryTag.count().desc(), diary.diaryDate.max().desc()) // 동률일 경우 가장 최근에 쓴 날짜를 기준으로 정렬
                .limit(10)
                .fetch();
    }
}
