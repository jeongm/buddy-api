package com.buddy.buddyapi.domain.auth.dto;

import com.buddy.buddyapi.domain.auth.enums.EmailPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "Email_VerifyRequest", description = "이메일 인증코드 검증 요청 DTO")
public record VerifyEmailRequest(
        @NotBlank String email,
        @Schema(description = "메일로 받은 6자리 인증코드", example = "123456")
        @NotBlank String code,
        @Schema(description = "인증 목적", example = "SIGNUP, PASSWORD_RESET")
        @NotNull EmailPurpose purpose
) {
}
