package com.buddy.buddyapi.domain.auth.dto;

import lombok.Builder;

public class AuthDto {

    private AuthDto() {}

    @Builder
    public record OauthLinkInfo(
            String email,
            String provider,
            String oauthId
    ) {}

    @Builder
    public record OAuthLinkRequest(
            String key // 프론트에서는 URL에서 뽑은 요 녀석만 보내면 됨!
    ) {}
}
