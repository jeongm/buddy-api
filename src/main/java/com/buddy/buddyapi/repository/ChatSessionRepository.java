package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.entity.ChatSession;
import com.buddy.buddyapi.entity.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    // 1. 특정 회원의 모든 대화 세션을 최신순으로 조회
    List<ChatSession> findAllByMemberOrderByCreatedAtDesc(Member member);

    // 2. 특정 회원의 세션 중 아직 종료되지 않은 (isEnded = false)
    // 사용자가 채팅창에 들어왔을 때 기존 대화를 이어붙여줄지 결정할 때 유용함
    Optional<ChatSession> findFirstByMemberAndIsEndedFalseOrderByCreatedAtDesc(Member member);

    // 3. 특정 회원의 특정 세션 조회 (내 세션이 맞는지 검증 포함)
    Optional<ChatSession> findBySessionSeqAndMember_MemberSeq(Long sessionSeq, Long memberSeq);

}
