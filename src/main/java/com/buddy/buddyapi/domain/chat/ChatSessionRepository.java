package com.buddy.buddyapi.domain.chat;

import com.buddy.buddyapi.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // 특정 회원의 세션 중 아직 종료되지 않은 (isEnded = false)
    // 사용자가 채팅창에 들어왔을 때 기존 대화를 이어붙여줄지 결정할 때 유용함
    Optional<ChatSession> findFirstByMemberAndIsEndedFalseOrderByCreatedAtDesc(Member member);

    // 특정 회원의 특정 세션 조회 (내 세션이 맞는지 검증 포함)
    Optional<ChatSession> findBySessionSeqAndMember_MemberSeq(Long sessionSeq, Long memberSeq);

}
