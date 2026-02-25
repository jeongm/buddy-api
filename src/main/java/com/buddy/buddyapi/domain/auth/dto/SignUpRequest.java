package com.buddy.buddyapi.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

        Long characterSeq
) {}