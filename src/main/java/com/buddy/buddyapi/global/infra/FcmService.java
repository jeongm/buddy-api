package com.buddy.buddyapi.global.infra;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    public void sendPush(String targetToken, String title, String body) {
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
                // .putData("link", "buddyapp://chat/123") // 딥링크 같은 추가 데이터도 넣을 수 있어요!
                .build();

        try {
            // Firebase 서버로 슝! 발송
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("🔔 푸시 알림 전송 성공: {}", response);
        } catch (Exception e) {
            log.error("푸시 알림 전송 실패: 토큰={}, 원인={}", targetToken, e.getMessage());
        }
    }
}