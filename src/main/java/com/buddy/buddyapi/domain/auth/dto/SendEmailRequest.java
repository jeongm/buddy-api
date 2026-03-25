package com.buddy.buddyapi.domain.auth.dto;

import com.buddy.buddyapi.domain.auth.enums.EmailPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "Email_SendRequest", description = "이메일 인증코드 발송 요청 DTO")
public record SendEmailRequest(
        @NotBlank String email,

        @NotNull
        @Schema(description = "인증 목적 (회원가입 or 비번찾기)", example = "SIGNUP, PASSWORD_RESET")
        EmailPurpose purpose
) {
}
