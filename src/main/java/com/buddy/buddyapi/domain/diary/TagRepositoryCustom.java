package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.domain.diary.dto.TagResponse;

import java.util.List;

public interface TagRepositoryCustom {
    List<TagResponse> findRecentTopTags(Long memberSeq);
}
