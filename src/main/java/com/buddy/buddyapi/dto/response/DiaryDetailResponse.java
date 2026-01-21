package com.buddy.buddyapi.dto.response;

import com.buddy.buddyapi.entity.Diary;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record DiaryDetailResponse(
        Long diarySeq,
        String title,
        String content,
        String imageUrl,
        LocalDateTime createdAt,
        List<TagResponse> tags // 태그도 seq와 name을 같이 주면 프론트가 편해요
) {
    public static DiaryDetailResponse from(Diary diary) {
        return DiaryDetailResponse.builder()
                .diarySeq(diary.getDiarySeq())
                .title(diary.getTitle())
                .content(diary.getContent())
                .imageUrl(diary.getImageUrl())
                .createdAt(diary.getCreatedAt())
                .tags(diary.getDiaryTags().stream()
                        .map(dt -> new TagResponse(dt.getTag().getTagSeq(), dt.getTag().getName()))
                        .toList())
                .build();
    }


}