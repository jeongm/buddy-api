package com.buddy.buddyapi.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "Auth_SignUpRequest", description = "일반 회원가입 요청 DTO")
public record SignUpRequest(

        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "이메일 형식이 유효하지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요")
        @Size(min = 8, max = 20, message = "비밀번호는 8자에서 20자 사이여야 합니다.")
        String password,

        @Schema(description = "이메일 인증 통과 시 발급받은 UUID 티켓")
        @NotBlank String verificationToken
) {
}
