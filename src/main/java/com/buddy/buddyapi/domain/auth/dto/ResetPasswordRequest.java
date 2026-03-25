package com.buddy.buddyapi.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 찾기 - 비밀번호 변경
 **/
public record ResetPasswordRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        String newPassword,

        @NotBlank(message = "인증 확인용 티켓은 필수입니다.")
        @Schema(description = "이메일 인증 통과 시 발급받은 UUID 티켓")
        String verificationToken
) {
}
