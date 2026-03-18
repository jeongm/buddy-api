package com.buddy.buddyapi.global.infra;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FcmService {

    /**
     * 한 명에게 푸시 알림
     */
    public void sendPushOne(String targetToken, String title, String body) {
        // 토큰이 없으면 보낼 수 없음
        if (targetToken == null || targetToken.isEmpty()) {
            return;
        }

        // Firebase로 보낼 메시지 조립
        Message message = Message.builder()
                .setToken(targetToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            // Firebase 서버로 슝! 발송
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("🔔 푸시 알림 전송 성공: {}", response);
        } catch (Exception e) {
            log.error("푸시 알림 전송 실패: 토큰={}, 원인={}", targetToken, e.getMessage());
        }
    }

    /**
     * 여러 명(최대 500명)에게 한 번의 API 호출로 푸시 알림
     */
    public void sendPushBulk(List<String> targetTokens, String title, String body) {
        if(targetTokens == null || targetTokens.isEmpty()) {
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(targetTokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("대량 푸시 알림 전송 완료! (성공: {}, 실패: {}",
                    response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("대량 푸시 알림 전송 실패: 원인={}", e.getMessage());
        }
    }

}