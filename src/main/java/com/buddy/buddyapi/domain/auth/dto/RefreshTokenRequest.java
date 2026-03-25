package com.buddy.buddyapi.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Auth_TokenRefreshRequest", description = "토큰 재발급 요청 DTO")
public record RefreshTokenRequest(
        @Schema(description = "기존 리프레시 토큰")
        String refreshToken
) {
}
