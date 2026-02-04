package com.buddy.buddyapi.dto.response;

import com.buddy.buddyapi.entity.Diary;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record DiaryDetailResponse(
        Long diarySeq,
        String title,
        String content,
        String imageUrl,
        LocalDate diaryDate,
        LocalDateTime createdAt,
        List<TagResponse> tags,
        Long sessionSeq

        ) {
    public static DiaryDetailResponse from(Diary diary) {
        return DiaryDetailResponse.builder()
                .diarySeq(diary.getDiarySeq())
                .title(diary.getTitle())
                .content(diary.getContent())
                .imageUrl(diary.getImageUrl())
                .diaryDate(diary.getDiaryDate())
                .createdAt(diary.getCreatedAt())
                .tags(diary.getDiaryTags().stream()
                        .map(dt -> new TagResponse(dt.getTag().getTagSeq(), dt.getTag().getName()))
                        .toList())
                .sessionSeq(diary.getChatSession() != null ? diary.getChatSession().getSessionSeq() : null)
                .build();
    }


}