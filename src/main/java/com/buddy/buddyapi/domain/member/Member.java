package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.domain.character.BuddyCharacter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="member")
@Entity
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(unique = true, nullable = false, length = 255)
    private String email;
    @Column(length = 255)
    private String password;
    @Column(nullable = false, length = 100)
    private String nickname;
    @CreationTimestamp // JPA/Hibernate에서 insert 시 자동으로 현재 시간을 채워줌
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "push_token", length = 255)
    private String pushToken;

    // 사용자는 직접 캐릭터의 별명을 지어줄 수 있음 추후 캐릭터 확장 시 테이블 분리
    @Column(name = "character_nickname", length = 20)
    private String characterNickname;

    // -------- relation --------

    // TODO 캐릭터 세계관 확장 시 분리하여 관리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private BuddyCharacter buddyCharacter;

    @Builder
    public Member(String email, String password, String nickname, BuddyCharacter buddyCharacter) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.buddyCharacter = buddyCharacter;
    }

    public void updatePassword(String password) {
        this.password = password;
    }
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
    public void changeCharacter(BuddyCharacter character) {
        this.buddyCharacter = character;
    }

    // 캐릭터 별명 변경 메서드
    public void updateCharacterNickname(String newNickname) {
        this.characterNickname = newNickname;
    }

    // 프론트에서 넘어온 새 토큰으로 갱신하는 메서드
    public void updatePushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public boolean isOnboardingCompleted() {
        return this.buddyCharacter != null;
    }
}
