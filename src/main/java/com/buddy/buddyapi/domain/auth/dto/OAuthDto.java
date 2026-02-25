package com.buddy.buddyapi.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class OAuthDto {

    private OAuthDto() {}

    public record OAuthLinkRequest(
            String key // 프론트에서는 URL에서 뽑은 요 녀석만 보내면 됨!
    ) {}

    public record LoginRequest(
            @NotBlank(message = "제공자(google, kakao, naver)는 필수입니다.")
            String provider,

            @NotBlank(message = "소셜 토큰은 필수입니다.")
            String token
    ){}



}
