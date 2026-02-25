package com.buddy.buddyapi.domain.auth;

import lombok.Builder;

@Builder
public record OAuthLinkInfo(
        String email,
        String provider,
        String oauthId
) {}