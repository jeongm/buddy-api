package com.buddy.buddyapi.domain.auth.dto;

public record WithdrawRequest(
        // 구글이나 네이버 유저가 탈퇴할 때만 프론트가 여기에 토큰을 담아서 보냅니다.
        // 카카오 유저나 일반 이메일 유저는 텅 비워서(null) 보내면 됩니다!
        String socialAccessToken
) {
}
