package com.buddy.buddyapi.global.security;

import com.buddy.buddyapi.domain.member.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public record CustomUserDetails(
        Long memberSeq,
        Long characterSeq,
        String email,
        String password, // 인증 시에만 사용됨
        Map<String, Object> attributes,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails, OAuth2User {

    // 일반 로그인용
    public static CustomUserDetails from(Member member) {

        Long charSeq = (member.getBuddyCharacter() != null)
                ? member.getBuddyCharacter().getCharacterSeq()
                : null;

        return new CustomUserDetails(
                member.getMemberSeq(),
                charSeq,
                member.getEmail(),
                member.getPassword(),
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // 소셜 로그인용
    public static CustomUserDetails of(Member member, Map<String,Object> attributes) {

        Long charSeq = (member.getBuddyCharacter() != null)
                ? member.getBuddyCharacter().getCharacterSeq()
                : null;

        return new CustomUserDetails(
                member.getMemberSeq(),
                charSeq,
                member.getEmail(),
                null,
                attributes,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // --- OAuth2User ---
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(memberSeq);
    }


    // --- UserDetails ---
    @Override
    public String getUsername() {
        return String.valueOf(memberSeq);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}