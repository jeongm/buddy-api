package com.buddy.buddyapi.entity;

import com.buddy.buddyapi.domain.Provider;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "oauth_account",
    uniqueConstraints = {
        @UniqueConstraint(name = "UX_oauth_provider_id", columnNames = {"provider", "oauthId"})
    }
)
@Entity
public class OauthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_account_seq")
    private Long OauthAccountSeq;

    @Enumerated(EnumType.STRING) // Enum타입을 DB에 문자열(Varchar)로 저장
    @Column(name = "provider", nullable = false, length = 10)
    private Provider provider;

    @Column(name = "oauth_id", nullable = false, length = 255)
    private String oauthId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @Builder
    public OauthAccount(Provider provider, String oauthId, Member member) {
        this.provider = provider;
        this.oauthId = oauthId;
        this.member = member;
    }



}
