package com.buddy.buddyapi.global.config;

import com.buddy.buddyapi.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public record CustomUserDetails(
        Long memberSeq,
        String email,
        String password, // 인증 시에만 사용됨
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    // 정적 팩토리 메서드
    public static CustomUserDetails from(Member member) {
        return new CustomUserDetails(
                member.getMemberSeq(),
                member.getEmail(),
                member.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // JSON으로 변환될 때(Jackson) password 필드가 무시되도록 설정하거나
    // 컨트롤러에서 사용할 때는 record의 getter를 호출하지 않으면 됩니다.

    // 인터페이스의 getAuthorities()를 호출하면 record의 authorities 필드를 반환합니다.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override public String getUsername() { return String.valueOf(memberSeq); }
    @Override public String getPassword() { return password; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}