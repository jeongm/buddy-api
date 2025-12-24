package com.buddy.buddyapi.dto.response;

import com.buddy.buddyapi.entity.Tag;

public record TagResponse(
        Long tagSeq,
        String name
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getTagSeq(), tag.getName());
    }
}