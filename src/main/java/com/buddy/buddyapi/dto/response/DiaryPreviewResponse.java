package com.buddy.buddyapi.dto.response;

import java.util.List;

public record DiaryPreviewResponse(
        String title,
        String content,
        List<TagResponse> tags
) {

}
