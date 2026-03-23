package com.buddy.buddyapi.domain.chat;

import com.buddy.buddyapi.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // 특정 회원의 세션 중 아직 종료되지 않은 (ended = false)
    // 사용자가 채팅창에 들어왔을 때 기존 대화를 이어붙여줄지 결정할 때 유용함
    Optional<ChatSession> findFirstByMemberAndEndedFalseOrderByCreatedAtDesc(Member member);

    // 특정 회원의 특정 세션 조회 (내 세션이 맞는지 검증 포함)
    Optional<ChatSession> findBySessionIdAndMember_MemberId(Long sessionId, Long memberId);

    // 알림 발송용 : 10시간 지남, 알림 안보냄, 일기로 안 만들어진 채팅방
    @Query("""
        SELECT c FROM ChatSession c 
        JOIN FETCH c.member 
        WHERE c.createdAt <= :tenHoursAgo 
          AND c.deletionNotifiedAt IS NULL 
          AND NOT EXISTS (SELECT 1 FROM Diary d WHERE d.chatSession = c)
        """)
    List<ChatSession> findWarningTargets(@Param("tenHoursAgo") LocalDateTime tenHoursAgo);

    // TODO 생각해보니까...ended 보고 결정하면 될듯 ended = false인 친구들만 삭제하면 되는거아냐?
    // 쓰레기 청소용 : 12시간 지남, 일기로 안 만들어진 채팅방 삭제
    // 벌크연산(Bulk Delete)으로 최적화(N+1)문제 방지
    // 쓰레기 청소 1단계: 자식(메시지) 먼저 삭제 (FK 에러 방지용)
    @Modifying(clearAutomatically = true)
    @Query("""
        DELETE FROM ChatMessage m 
        WHERE m.chatSession.sessionId IN (
            SELECT c.sessionId FROM ChatSession c 
            WHERE c.createdAt <= :twelveHoursAgo 
              AND NOT EXISTS (SELECT 1 FROM Diary d WHERE d.chatSession = c)
        )
        """)
    int deleteOrphanMessages(@Param("twelveHoursAgo") LocalDateTime twelveHoursAgo);

    // 쓰레기 청소 2단계: 부모(세션) 삭제
    @Modifying(clearAutomatically = true)
    @Query("""
        DELETE FROM ChatSession c 
        WHERE c.createdAt <= :twelveHoursAgo 
          AND NOT EXISTS (SELECT 1 FROM Diary d WHERE d.chatSession = c)
        """)
    int deleteOrphanSessions(@Param("twelveHoursAgo") LocalDateTime twelveHoursAgo);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ChatSession cs WHERE cs.member.memberId = :memberId")
    void bulkDeleteByMemberId(@Param("memberId") Long memberId);

    // 여러 채팅 세션의 알림 발송 시간을 현재 시간으로 한 번에(Bulk) 업데이트
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatSession cs SET cs.deletionNotifiedAt = CURRENT_TIMESTAMP WHERE cs.sessionId IN :sessionIds")
    void bulkMarkAsNotified(@Param("sessionIds") List<Long> sessionIds);

}
