package com.buddy.buddyapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CharacterNameRequest(
        @NotBlank(message = "캐릭터 이름을 입력해주세요")
        @Size(min = 1, max = 20, message = "이름은 1~20자 사이입니다.")
        String characterName
) {
}
