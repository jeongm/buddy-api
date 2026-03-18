package com.buddy.buddyapi.domain.diary.event;

import java.util.List;

public record DiaryImagesCleanupEvent(
        List<String> imageUrls
) {
}
