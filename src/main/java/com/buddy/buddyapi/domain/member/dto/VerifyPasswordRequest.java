package com.buddy.buddyapi.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPasswordRequest(
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword
) {
}
