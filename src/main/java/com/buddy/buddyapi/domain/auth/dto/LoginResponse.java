package com.buddy.buddyapi.domain.auth.dto;

import com.buddy.buddyapi.domain.auth.AuthStatus;
import com.buddy.buddyapi.domain.member.dto.MemberResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 응답에서 아예 빼버리는 센스!
public record LoginResponse(
        AuthStatus status,
        String accessToken, // SUCCESS일 때만
        String refreshToken,
        MemberResponse member,
        String linkKey // REQUIRES_LINKING일 때만
) {
}
