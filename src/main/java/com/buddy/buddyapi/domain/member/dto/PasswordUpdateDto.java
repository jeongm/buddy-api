package com.buddy.buddyapi.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public class PasswordUpdateDto {
    // 1단계: 현재 비밀번호 검증용 DTO
    public record VerifyRequest(
            @NotBlank(message = "현재 비밀번호를 입력해주세요.")
            String currentPassword
    ) {}

    // 2단계: 실제 비밀번호 변경용 DTO
    public record UpdateRequest(
            @NotBlank(message = "현재 비밀번호를 입력해주세요.")
            String currentPassword,

            @NotBlank(message = "새 비밀번호를 입력해주세요.")
            String newPassword
    ) {}
}
