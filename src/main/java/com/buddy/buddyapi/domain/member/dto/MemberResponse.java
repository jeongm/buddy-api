package com.buddy.buddyapi.domain.member.dto;

import com.buddy.buddyapi.domain.member.Member;
import lombok.Builder;

@Builder
public record MemberResponse(
        Long memberSeq,
        String email,
        String nickname,
        Long characterSeq,
        String characterNickname,
        String avatarUrl
) {

    public static MemberResponse from(Member member) {

        return MemberResponse.builder()
                .memberSeq(member.getMemberSeq())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .characterSeq(member.getBuddyCharacter().getCharacterSeq())
                .characterNickname(member.getCharacterNickname() != null
                        ? member.getCharacterNickname(): member.getBuddyCharacter().getName())
                .avatarUrl(member.getBuddyCharacter() != null ? member.getBuddyCharacter().getAvatarUrl() : null)
                .build();
    }
}
