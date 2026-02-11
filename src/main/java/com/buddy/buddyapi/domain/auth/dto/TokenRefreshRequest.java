package com.buddy.buddyapi.domain.auth.dto;

public record TokenRefreshRequest(
        String refreshToken
) {
}
