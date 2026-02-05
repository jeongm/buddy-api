package com.buddy.buddyapi.dto.response;

import com.buddy.buddyapi.entity.Member;

public record MemberSeqResponse(
        Long memberSeq
) {
    public static MemberSeqResponse from(Member member) {
        return new MemberSeqResponse(member.getMemberSeq());
    }
}
