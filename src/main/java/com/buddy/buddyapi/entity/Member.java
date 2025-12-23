package com.buddy.buddyapi.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="member")
@Entity
public class Member implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    // -------- relation --------

    @ManyToOne(fetch = FetchType.LAZY)
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


    // --- UserDetails 인터페이스 구현 메서드

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 여부 (true: 만료 안됨)
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠김 여부 (true: 잠기지 않음)
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료 여부 (true: 만료 안됨)
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 활성화 여부 (true: 활성화)
    }
}
