package com.buddy.buddyapi.domain.diary.dto;

import com.buddy.buddyapi.domain.diary.Tag;

public record TagResponse(
        Long tagId,
        String name
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getTagId(), tag.getName());
    }
}