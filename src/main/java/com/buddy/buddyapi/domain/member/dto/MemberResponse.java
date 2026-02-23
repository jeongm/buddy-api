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

        var buddy = member.getBuddyCharacter();

        return MemberResponse.builder()
                .memberSeq(member.getMemberSeq())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .characterSeq(buddy != null ? buddy.getCharacterSeq() : null)
                .characterNickname(member.getCharacterNickname() != null
                        ? member.getCharacterNickname()
                        : (buddy != null ? buddy.getName() : null))
                .avatarUrl(buddy != null ? member.getBuddyCharacter().getAvatarUrl() : null)
                .build();
    }
}
