package com.buddy.buddyapi.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompleteOnboardingRequest(
        @NotBlank(message = "닉네임을 입력해주세요")
        String nickname,

        @NotNull(message = "캐릭터를 선택해주세요")
        Long characterId,

        @NotBlank(message = "캐릭터 이름을 입력해주세요")
        @Size(min = 1, max = 20, message = "이름은 1~20자 사이입니다.")
        String characterName,

        @NotNull(message = "야간 알림 동의 여부를 선택해주세요")
        Boolean isNightAgreed
) {}