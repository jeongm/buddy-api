package com.buddy.buddyapi.domain.auth.dto;

import com.buddy.buddyapi.domain.auth.enums.EmailPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EmailDto {

    @Schema(name = "Email_SendRequest", description = "이메일 인증코드 발송 요청 DTO")
    public record SendRequest(
            @NotBlank String email,

            @NotNull
            @Schema(description = "인증 목적 (회원가입 or 비번찾기)", example = "SIGNUP, PASSWORD_RESET")
            EmailPurpose purpose
    ) {
    }

    @Schema(name = "Email_VerifyRequest", description = "이메일 인증코드 검증 요청 DTO")
    public record VerifyRequest(
            @NotBlank String email,
            @Schema(description = "메일로 받은 6자리 인증코드", example = "123456")
            @NotBlank String code,
            @Schema(description = "인증 목적", example = "SIGNUP")
            @NotNull EmailPurpose purpose
    ) {
    }

    /**
     * 비밀번호 찾기 - 비밀번호 변경
     **/
    public record PasswordResetRequest(
            @NotBlank(message = "이메일을 입력해주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @NotBlank(message = "새 비밀번호를 입력해주세요.")
            String newPassword,

            @NotBlank(message = "인증 확인용 티켓은 필수입니다.")
            @Schema(description = "이메일 인증 통과 시 발급받은 UUID 티켓")
            String verificationToken
    ) {}


}
