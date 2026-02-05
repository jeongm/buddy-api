package com.buddy.buddyapi.dto.response;

import com.buddy.buddyapi.entity.Diary;

import java.time.LocalDateTime;
import java.util.List;

public record DiaryListResponse(
        Long diarySeq,
        String title,
        String summary,
        LocalDateTime createAt,
        String imageUrl,
        List<String> tags

) {
    public static DiaryListResponse from(Diary diary) {
        String content = diary.getContent();
        String summary = (content != null && content.length() > 60)
                ? content.substring(0,60) + "..." : content;

        return new DiaryListResponse(
                diary.getDiarySeq(),
                diary.getTitle(),
                summary,
                diary.getCreatedAt(),
                diary.getImageUrl(),
                diary.getDiaryTags().stream()
                        .map(diaryTag -> diaryTag.getTag().getName())
                        .toList()
        );
    }
}
