package com.buddy.buddyapi.domain.auth.dto;

public record EmailVerifyRequest(
        String email,
        String code
) {
}
