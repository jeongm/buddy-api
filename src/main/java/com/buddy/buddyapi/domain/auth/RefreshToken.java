package com.buddy.buddyapi.domain.auth;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 1209600)
public class RefreshToken {

    @Id
    private Long memberId;

    @Indexed
    private String refreshToken;

    @TimeToLive
    private Long expiration;
}
