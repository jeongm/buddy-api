package com.buddy.buddyapi.domain.auth.dto;

public record OAuthLinkRequest(
        String email,
        String provider,
        String oauthId
) {
}
