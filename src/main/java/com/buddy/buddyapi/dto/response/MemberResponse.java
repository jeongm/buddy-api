package com.buddy.buddyapi.dto.response;

import com.buddy.buddyapi.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponse {

    private Long memberSeq;
    private String email;
    private String nickname;

    // TODO 캐릭터에 관한거는
    private Integer characterSeq;
    private String characterName;
    private String avatarUrl;

    public static MemberResponse from(Member member) {
        // Entity에서 비밀번호 등 민감한 필드는 제외하고, 필요한 정보만 추출
        return MemberResponse.builder()
                .memberSeq(member.getMemberSeq())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .characterSeq(member.getBuddyCharacter() != null ? member.getBuddyCharacter().getCharacterSeq() : null)
                .characterName(member.getBuddyCharacter() != null ? member.getBuddyCharacter().getName() : null)
                .avatarUrl(member.getBuddyCharacter() != null ? member.getBuddyCharacter().getAvatarUrl() : null)
                .build();
    }
}
