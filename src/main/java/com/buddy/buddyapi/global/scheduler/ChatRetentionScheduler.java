package com.buddy.buddyapi.global.scheduler;

import com.buddy.buddyapi.domain.chat.ChatSessionRepository;
import com.buddy.buddyapi.domain.chat.dto.PushTargetDto;
import com.buddy.buddyapi.global.infra.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
     * - 추후 사용자 증가 시 Spring Batch 도입할 것 (현재는 소규모이므로 100개씩만 처리하도록 함)
     */
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void sendWarningPushNotifications() {
        LocalDateTime tenHoursAgo = LocalDateTime.now().minusHours(10);

        List<PushTargetDto> targets = chatSessionRepository.findWarningTargets(
                tenHoursAgo, PageRequest.of(0,100)
        );

        if (targets.isEmpty()) return;

        List<Long> targetSessionIds = targets.stream()
                .map(PushTargetDto::sessionId)
                .toList();

        List<String> targetTokens = targets.stream()
                .map(PushTargetDto::pushToken)
                .filter(token -> token != null && !token.isBlank())
                .toList();

        // 발송 시간 업데이트
        chatSessionRepository.bulkMarkAsNotified(targetSessionIds);

        // 알림 발송
        if (!targetTokens.isEmpty()) {
            try {
                fcmService.sendPushBulk(targetTokens,
                        "버디가 기다리고 있어요",
                        "대화가 곧 지워질 예정이에요 일기로 남겨볼까요?");
                log.info("PUSH 알림 발송 완료 - 총 발송 유저 수: {}명", targetTokens.size());

            } catch (Exception e) {
                log.error("FCM 푸시 발송 실패 (대상 수: {})", targetTokens.size(), e);
            }
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

        int deletedSessions = chatSessionRepository.deleteOrphanSessions(twelveHoursAgo);

        if(deletedSessions > 0) {
            log.info("잉여 메시지 청소 완료: 12시간이 경과된 세션 {}개를 영구 삭제했습니다.", deletedSessions);
        }
    }



}
