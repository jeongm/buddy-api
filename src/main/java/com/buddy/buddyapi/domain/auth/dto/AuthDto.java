package com.buddy.buddyapi.domain.auth.dto;

import com.buddy.buddyapi.domain.auth.enums.AuthStatus;
import com.buddy.buddyapi.domain.member.dto.MemberResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public class AuthDto {

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 응답에서 아예 빼버리는 센스!
    public record LoginResponse(
            AuthStatus status,
            String accessToken, // SUCCESS일 때만
            String refreshToken,
            MemberResponse member,
            String linkKey // REQUIRES_LINKING일 때만
    ) {
    }

    @Schema(name = "Auth_SignUpRequest", description = "일반 회원가입 요청 DTO")
    public record SignUpRequest(

            @NotBlank(message = "이메일은 필수 입력 항목입니다.")
            @Email(message = "이메일 형식이 유효하지 않습니다.")
            String email,

            @NotBlank(message = "비밀번호를 입력해 주세요")
            @Size(min = 8, max = 20, message = "비밀번호는 8자에서 20자 사이여야 합니다.")
            String password,

            @NotBlank(message = "닉네임을 입력해주세요")
            @Size(min = 1, max = 15, message = "닉네임은 15자 이하여야 합니다.")
            String nickname,

            Long characterId,

            @Schema(description = "이메일 인증 통과 시 발급받은 UUID 티켓")
            @NotBlank String verificationToken
    ) {}

    @Schema(name = "Auth_EmailLoginRequest", description = "일반 로그인 요청 DTO")
    public record EmailLoginRequest(
            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @NotBlank(message = "비밀번호를 입력해 주세요.")
            String password
    ) {}

    @Schema(name = "Auth_TokenRefreshRequest", description = "토큰 재발급 요청 DTO")
    public record TokenRefreshRequest(
            @Schema(description = "기존 리프레시 토큰")
            String refreshToken
    ) {
    }

}
