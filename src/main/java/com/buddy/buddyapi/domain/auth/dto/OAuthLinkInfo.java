package com.buddy.buddyapi.domain.auth.dto;

import lombok.Builder;

@Builder
public record OAuthLinkInfo(
        String email,
        String provider,
        String oauthId,
        String socialAccessToken,
        String socialRefreshToken
) {}