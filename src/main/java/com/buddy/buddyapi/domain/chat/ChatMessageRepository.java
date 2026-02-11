package com.buddy.buddyapi.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 특정 세션의 모든 메시지를 과거 -> 최신순으로 정렬하여 조회
    // (AI에게 문맥을 전달하거나 채팅방에 진입할 때 사용)
    List<ChatMessage> findAllByChatSessionOrderByCreatedAtDesc(ChatSession chatSession);

    // 2. 특정 세션의 메시지 개수 확인 (세션의 활성화 정도 파악용)
//    long countByChatSession(ChatSession chatSession);

    // 3. (옵션) 특정 세션의 가장 마지막 메시지 하나만 조회
//    Optional<ChatMessage> findFirstByChatSessionOrderByCreatedAtDesc(ChatSession chatSession);

    List<ChatMessage>  findAllByChatSessionOrderByCreatedAtAsc(ChatSession chatSession);
}
