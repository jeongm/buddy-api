package com.buddy.buddyapi.domain.insight.dto;

public record TagNameCountResponse(
        String tagName,
        Long count
) {
}
