package com.buddy.buddyapi.domain.auth.dto;

import com.buddy.buddyapi.domain.member.dto.MemberResponse;
import lombok.Builder;

@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken,
        MemberResponse member
) {
}
