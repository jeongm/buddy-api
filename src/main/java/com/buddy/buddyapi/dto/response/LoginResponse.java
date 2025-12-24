package com.buddy.buddyapi.dto.response;

import lombok.Builder;

@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken,
        MemberResponse member
) {
}
