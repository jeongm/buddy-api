package com.buddy.buddyapi.domain.auth.dto;

public record LinkOAuthRequest(
        String key // 프론트에서는 URL에서 뽑은 요 녀석만 보내면 됨!
) {
}
