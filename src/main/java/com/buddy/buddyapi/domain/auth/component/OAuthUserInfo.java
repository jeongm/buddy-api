package com.buddy.buddyapi.domain.auth.component;

public record OAuthUserInfo(
        String email,
        String name,
        String oauthId
) {
}
