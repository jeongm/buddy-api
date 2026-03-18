package com.buddy.buddyapi.domain.member.dto;

import com.buddy.buddyapi.domain.member.Member;
import lombok.Builder;

@Builder
public record MemberResponse(
        Long memberId,
        String email,
        String nickname,
        Long characterId,
        String characterNickname,
        String avatarUrl
) {

    public static MemberResponse from(Member member) {

        var buddy = member.getBuddyCharacter();

        return MemberResponse.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .characterId(buddy != null ? buddy.getCharacterId() : null)
                .characterNickname(member.getCharacterNickname() != null
                        ? member.getCharacterNickname()
                        : (buddy != null ? buddy.getName() : null))
                .avatarUrl(buddy != null ? member.getBuddyCharacter().getAvatarUrl() : null)
                .build();
    }
}
