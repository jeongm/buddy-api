package com.buddy.buddyapi.domain.diary.dto;

import java.util.List;

public record DiaryPreviewResponse(
        String title,
        String content,
        List<TagResponse> tags
) {

}
