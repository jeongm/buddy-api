package com.buddy.buddyapi.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PasswordResetDto {

    // 1단계: 이메일 전송 요청
    public record SendCodeRequest(
            @NotBlank(message = "이메일을 입력해주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email
    ) {}

    // 2단계: 실제 비밀번호 변경 요청
    public record ResetRequest(
            @NotBlank(message = "이메일을 입력해주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @NotBlank(message = "인증번호를 입력해주세요.")
            @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 6자리 숫자여야 합니다.")
            String code,

            @NotBlank(message = "새 비밀번호를 입력해주세요.")
            String newPassword
    ) {}
}