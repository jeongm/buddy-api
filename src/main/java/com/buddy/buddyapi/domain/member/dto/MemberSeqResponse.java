package com.buddy.buddyapi.domain.member.dto;

import com.buddy.buddyapi.domain.member.Member;

public record MemberSeqResponse(
        Long memberSeq
) {
    public static MemberSeqResponse from(Member member) {
        return new MemberSeqResponse(member.getMemberSeq());
    }
}
