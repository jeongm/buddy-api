package com.buddy.buddyapi.domain.diary.event;

import org.springframework.web.multipart.MultipartFile;

public record DiaryImageUpdateEvent(
        Long diaryId,
        String oldImageUrl,
        MultipartFile newImage
) {
}
