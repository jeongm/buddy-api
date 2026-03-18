package com.buddy.buddyapi.domain.member.event;

import com.buddy.buddyapi.domain.member.Provider;

import java.util.List;

/**
 * 트랜잭션 커밋 후 소셜 연동 해제 API를 호출하기 위한 이벤트.
 * 엔티티 대신 필요한 값만 담아 세션 종료 후 LazyInitializationException을 방지합니다.
 */
public record SocialUnlinkEvent(
        List<AccountSnapshot> accounts
) {
    public record AccountSnapshot(
            Provider provider,
            String oauthId,
            String accessToken,
            String refreshToken
    ) {}
}
