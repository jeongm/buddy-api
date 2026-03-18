package com.buddy.buddyapi.global.scheduler;

import com.buddy.buddyapi.domain.chat.ChatSession;
import com.buddy.buddyapi.domain.chat.ChatSessionRepository;
import com.buddy.buddyapi.global.infra.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChatRetentionScheduler {

    private final ChatSessionRepository chatSessionRepository;
    private final FcmService fcmService;

    /**
     * 1. 알림 스케줄러: 매시간 30분마다 실행 (예: 1시 30분, 2시 30분)
     * - 10시간 경과 & 일기 미생성 & 알림 안 보낸 세션 찾아서 알림 발송
     */
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void sendWarningPushNotifications() {
        LocalDateTime tenHoursAgo = LocalDateTime.now().minusHours(10);

        List<ChatSession> warningTargets = chatSessionRepository.findWarningTargets(tenHoursAgo);

        if (warningTargets.isEmpty()) {
            return;
        }

        List<String> targetTokens = warningTargets.stream()
                .map(session -> session.getMember().getPushToken())
                .filter(token -> token != null && !token.isBlank())
                .toList();

        List<Long> targetSessionIds = warningTargets.stream()
                .map(ChatSession::getSessionId)
                .toList();

        if(!targetTokens.isEmpty()) {
            fcmService.sendPushBulk(targetTokens, "버디가 기다리고 있어요", "대화가 곧 지워질 예정이에요 일기로 남겨볼까요?");
            log.info("PUSH 알림 발송 대상 유저 수: {}명", targetTokens.size());
        }


        // 알림 발송 완료 처리 - 발송 시간 업데이트
        if(!targetSessionIds.isEmpty()) {
            chatSessionRepository.bulkMarkAsNotified(targetSessionIds);
        }
    }

    /**
     * 청소 스케줄러: 매시간 정각마다 실행 (예: 1시 00분, 2시 00분)
     * - 12시간 경과 & 일기 미생성 세션 완전 삭제 (벌크 연산)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanUpOrphanChatSessions(){
        LocalDateTime twelveHoursAgo = LocalDateTime.now().minusHours(12);

        int deletedMessages = chatSessionRepository.deleteOrphanMessages(twelveHoursAgo);
        int deletedSessions = chatSessionRepository.deleteOrphanSessions(twelveHoursAgo);

        if(deletedSessions > 0) {
            log.info("잉여 메시지 청소 완료: 12시간이 경과된 채팅 메시지{}개, 세션 {}개를 영구 삭제했습니다.", deletedMessages, deletedSessions);
        }
    }



}
