package com.buddy.buddyapi.domain.member;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "oauth_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "UX_oauth_provider_id", columnNames = {"provider", "oauth_id"})
        },
        indexes = {
                @Index(name = "IX_oauth_account_member", columnList = "member_id")
        }
)
@Entity
public class OauthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_account_id")
    private Long oauthAccountId;

    @Enumerated(EnumType.STRING) // Enum타입을 DB에 문자열(Varchar)로 저장
    @Column(name = "provider", nullable = false, length = 10)
    private Provider provider;

    @Column(name = "oauth_id", nullable = false, length = 255)
    private String oauthId;

    // 소셜 액세스 토큰 (1~2시간 뒤 만료됨)
    @Column(name = "social_access_token", columnDefinition = "TEXT")
    private String socialAccessToken;

    // 소셜 리프레시 토큰 (나중에 탈퇴할 때 새 액세스 토큰으로 교환할 티켓)
    @Column(name = "social_refresh_token", columnDefinition = "TEXT")
    private String socialRefreshToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Builder
    public OauthAccount(Provider provider, String oauthId, String socialAccessToken, String socialRefreshToken, Member member) {
        this.provider = provider;
        this.oauthId = oauthId;
        this.socialAccessToken = socialAccessToken;
        this.socialRefreshToken = socialRefreshToken;
        this.member = member;
    }

    public void updateTokens(String accessToken, String refreshToken) {
        this.socialAccessToken = accessToken;
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.socialRefreshToken = refreshToken;
        }
    }



}
