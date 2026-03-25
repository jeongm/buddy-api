package com.buddy.buddyapi.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

// 1단계: 현재 비밀번호 검증용 DTO
public record VerifyPasswordRequest(
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword
) {
}
