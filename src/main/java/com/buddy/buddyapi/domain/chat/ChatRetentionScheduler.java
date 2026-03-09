package com.buddy.buddyapi.domain.chat;

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
//    @Scheduled(fixedDelay = 10000) // 테스트용 10초마다 알림
    @Transactional
    public void sendWarningPushNotifications() {
//        LocalDateTime tenHoursAgo = LocalDateTime.now().minusMinutes(1);
        LocalDateTime tenHoursAgo = LocalDateTime.now().minusHours(10);

        List<ChatSession> warningTargets = chatSessionRepository.findWarningTargets(tenHoursAgo);

        if (warningTargets.isEmpty()) {
            return;
        }

        for (ChatSession session : warningTargets) {
            // 🌟 1. 해당 세션을 만든 유저의 기기 토큰 가져오기
            String targetToken = session.getMember().getPushToken();

            // 🌟 2. 토큰이 정상적으로 있을 때만 알림 쏘기
            if (targetToken != null && !targetToken.isBlank()) {
                String title = "버디가 기다리고 있어요! 🥺";
                String body = "대화가 곧 지워질 예정이에요. 대화를 일기로 저장해볼까요?";

                // 👉 추가할 로그: 어떤 토큰으로 쏘는지 확인!
                log.info("🎯 푸시 알림 발송 시도! 대상 토큰: {}", targetToken);

                // 나중에 딥링크(특정 화면으로 이동)가 필요하면 FcmService를 수정해서 넘겨주면 됩니다.
                fcmService.sendPush(targetToken, title, body);
            }
            // 알림 보낸 시간 저장
            session.markDeletionNotified();

        }

        log.info("🔔 소멸 경고 알림 발송 대상: {}개의 채팅방", warningTargets.size());
    }

    /**
     * 2. 청소 스케줄러: 매시간 정각마다 실행 (예: 1시 00분, 2시 00분)
     * - 12시간 경과 & 일기 미생성 세션 완전 삭제 (벌크 연산)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanUpOrphanChatSessions(){
        LocalDateTime twelveHoursAgo = LocalDateTime.now().minusHours(12);

        int deletedMessages = chatSessionRepository.deleteOrphanMessages(twelveHoursAgo);
        int deletedSessions = chatSessionRepository.deleteOrphanSessions(twelveHoursAgo);

        if(deletedSessions > 0) {
            log.info("🧹 잉여 메시지 청소 완료: 12시간이 경과된 채팅 메시지{}개, 세션 {}개를 영구 삭제했습니다.", deletedMessages, deletedSessions);
        }
    }
}
