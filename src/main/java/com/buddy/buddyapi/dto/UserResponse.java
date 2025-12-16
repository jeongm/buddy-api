package com.buddy.buddyapi.dto;

import com.buddy.buddyapi.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long userSeq;
    private String email;
    private String nickname;

    // TODO 캐릭터에 관한거는
    private Integer characterSeq;
    private String characterName;
    private String avatarUrl;

    public static UserResponse from(User user) {
        // Entity에서 비밀번호 등 민감한 필드는 제외하고, 필요한 정보만 추출
        return UserResponse.builder()
                .userSeq(user.getUserSeq())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .characterSeq(user.getBuddyCharacter() != null ? user.getBuddyCharacter().getCharacterSeq() : null)
                .characterName(user.getBuddyCharacter() != null ? user.getBuddyCharacter().getName() : null)
                .avatarUrl(user.getBuddyCharacter() != null ? user.getBuddyCharacter().getAvatarUrl() : null)
                .build();
    }
}
