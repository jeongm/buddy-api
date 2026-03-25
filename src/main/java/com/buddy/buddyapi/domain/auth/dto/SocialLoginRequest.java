package com.buddy.buddyapi.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        @NotBlank(message = "제공자(google, kakao, naver, apple)는 필수입니다.")
        String provider,

        @NotBlank(message = "소셜 토큰은 필수입니다.")
        String token // Google: idToken / Kakao·Naver: authCode / Apple: identityToken
) {
}
