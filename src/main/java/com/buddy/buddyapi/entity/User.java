package com.buddy.buddyapi.entity;

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
@Table(name="users")
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userSeq;

    @Column(unique = true, nullable = false, length = 255)
    private String email;
    @Column(length = 255)
    private String password;
    @Column(nullable = false, length = 100)
    private String nickname;
    @CreationTimestamp // JPA/Hibernate에서 insert 시 자동으로 현재 시간을 채워줌
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    // -------- relation --------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_seq",nullable = false)
    private BuddyCharacter buddyCharacter;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OauthAccount> oauthAccounts = new ArrayList<>();
    
    @OneToMany(mappedBy = "user")
    private final List<Diary> diaries = new ArrayList<>();

    @Builder
    public User(String email, String password, String nickname, BuddyCharacter buddyCharacter) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.buddyCharacter = buddyCharacter;
    }
    
    // TODO 수정 - 소셜로그인 관련 로직 추가

    public void updateBuddyCharacter(BuddyCharacter buddyCharacter) {
        this.buddyCharacter = buddyCharacter;
    }
    public void updatePassword(String password) {
        this.password = password;
    }
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }


}
