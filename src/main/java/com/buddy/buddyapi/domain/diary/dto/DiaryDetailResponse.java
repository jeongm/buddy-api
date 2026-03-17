package com.buddy.buddyapi.domain.diary.dto;

import com.buddy.buddyapi.domain.diary.Diary;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record DiaryDetailResponse(
        Long diaryId,
        String title,
        String content,
        String imageUrl,
        LocalDate diaryDate,
        LocalDateTime createdAt,
        List<TagResponse> tags,
        Long sessionId

        ) {
    public static DiaryDetailResponse from(Diary diary) {
        return DiaryDetailResponse.builder()
                .diaryId(diary.getDiaryId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .imageUrl(diary.getImageUrl())
                .diaryDate(diary.getDiaryDate())
                .createdAt(diary.getCreatedAt())
                .tags(diary.getDiaryTags().stream()
                        .map(dt -> new TagResponse(dt.getTag().getTagId(), dt.getTag().getName()))
                        .toList())
                .sessionId(diary.getChatSession() != null ? diary.getChatSession().getSessionId() : null)
                .build();
    }


}