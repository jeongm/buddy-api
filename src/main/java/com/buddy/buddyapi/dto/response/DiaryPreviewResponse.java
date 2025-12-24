package com.buddy.buddyapi.dto.response;

import java.util.List;

public record DiaryPreviewResponse(
        // TODO AI로 일기 생성 시 초안, 실제 채팅 연동 후 변경 예정
        String title,
        String content,
        List<Long> tags
) {

}
