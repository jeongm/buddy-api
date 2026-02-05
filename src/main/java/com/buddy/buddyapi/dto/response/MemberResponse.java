package com.buddy.buddyapi.dto.response;

import com.buddy.buddyapi.entity.BuddyCharacter;
import com.buddy.buddyapi.entity.Member;
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
        // 변수명을 character라고 지었으니 누가 봐도 BuddyCharacter 타입
        // var는 java10에서 도입되어 컴파일러가 오른쪽의 대입되는 값을 보고 타입을 자동으로 알아냄(지역변수타입추론 local variable type inference)
//        var character = member.getBuddyCharacter();
        // TODO N+1 발생 서비스로직에서 캐릭터 까지 들고오도록 호출해야할듯
        BuddyCharacter character = member.getBuddyCharacter();

        // Entity에서 비밀번호 등 민감한 필드는 제외하고, 필요한 정보만 추출
        return MemberResponse.builder()
                .memberSeq(member.getMemberSeq())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .characterSeq(character.getCharacterSeq())
                .characterNickname(member.getCharacterNickname() != null
                        ? member.getCharacterNickname(): character.getName())
                .avatarUrl(member.getBuddyCharacter() != null ? member.getBuddyCharacter().getAvatarUrl() : null)
                .build();
    }
}
