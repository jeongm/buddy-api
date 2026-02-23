package com.buddy.buddyapi.domain.auth.dto;

import lombok.Builder;

public record AuthDto() {
    @Builder
    public record SuccessResponse(
            String accessToken,
            String refreshToken,
            boolean isNewMember
    ) {
    }

    @Builder
    public record LinkResponse(
            String email,
            String provider,
            String oauthId,
            String linkToken
    ) {
    }

    @Builder
    public record OAuthLinkRequest(
            String key // 프론트에서는 URL에서 뽑은 요 녀석만 보내면 됨!
    ) {}
}
