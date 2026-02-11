package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.domain.character.BuddyCharacter;
import com.buddy.buddyapi.domain.diary.Diary;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="member")
@Entity
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_seq")
    private Long memberSeq;

    @Column(unique = true, nullable = false, length = 255)
    private String email;
    @Column(length = 255)
    private String password;
    @Column(nullable = false, length = 100)
    private String nickname;
    @CreationTimestamp // JPA/Hibernate에서 insert 시 자동으로 현재 시간을 채워줌
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    // 사용자는 직접 캐릭터의 별명을 지어줄 수 있음
    @Column(name = "character_nickname", length = 20)
    private String characterNickname;

    // -------- relation --------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_seq",nullable = false)
    private BuddyCharacter buddyCharacter;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OauthAccount> oauthAccounts = new ArrayList<>();
    
    @OneToMany(mappedBy = "member")
    private final List<Diary> diaries = new ArrayList<>();

    @Builder
    public Member(String email, String password, String nickname, BuddyCharacter buddyCharacter) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.buddyCharacter = buddyCharacter;
    }
    

    public void updateBuddyCharacter(BuddyCharacter buddyCharacter) {
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


}
