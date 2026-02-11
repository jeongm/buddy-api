package com.buddy.buddyapi.dto.request;

public record OAuthLinkRequest(
        String email,
        String provider,
        String oauthId
) {
}
