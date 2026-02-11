package com.buddy.buddyapi.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CharacterChangeRequest(
        @Schema(description = "변경할 캐릭터 식별자", example = "1")
        Long characterSeq
) {
}
