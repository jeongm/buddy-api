package com.buddy.buddyapi.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public class OAuthDto {

    private OAuthDto() {}

    @Builder
    public record OauthLinkInfo(
            String email,
            String provider,
            String oauthId
    ) {}


    public record OAuthLinkRequest(
            String key // 프론트에서는 URL에서 뽑은 요 녀석만 보내면 됨!
    ) {}

    // ==========================================================
    // 🚀 (신규) 앱/웹 공용 토큰 기반 소셜 로그인용 DTO
    // ==========================================================

    public record LoginRequest(
            @NotBlank(message = "제공자(google, kakao, naver)는 필수입니다.")
            String provider,

            @NotBlank(message = "소셜 토큰은 필수입니다.")
            String token
    ){}



}
