package com.buddy.buddyapi.global.infra;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FcmService {

    /**
     * 단일 대상에게 푸시 알림을 전송합니다.
     *
     * @param targetToken FCM 디바이스 토큰
     * @param title       알림 제목
     * @param body        알림 본문
     */
    public void sendPushOne(String targetToken, String title, String body) {
        // 토큰이 없으면 보낼 수 없음
        if (targetToken == null || targetToken.isEmpty()) {
            log.warn("sendPushOne 호출 시 토큰이 null 또는 빈 값입니다. 발송 생략.");
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
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("단일 푸시 알림 전송 성공 - messageId: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("❌ 단일 푸시 전송 실패 - token: {}, errorCode: {}, message: {}",
                    maskToken(targetToken), e.getMessagingErrorCode(), e.getMessage());
        }
    }

    /**
     * 여러 대상에게 멀티캐스트 푸시 알림을 전송합니다. (FCM 최대 500명 제한)
     * 개별 토큰의 실패 원인을 상세히 로깅합니다.
     *
     * @param targetTokens FCM 디바이스 토큰 목록
     * @param title        알림 제목
     * @param body         알림 본문
     */
    public void sendPushBulk(List<String> targetTokens, String title, String body) {
        if(targetTokens == null || targetTokens.isEmpty()) {
            log.warn("sendPushBulk 호출 시 토큰 목록이 비어있습니다. 발송 생략.");
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
            BatchResponse batchResponse = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("대량 푸시 알림 전송 완료! (성공: {}, 실패: {}",
                    batchResponse.getSuccessCount(), batchResponse.getFailureCount());

            // 실패가 있을 때만 원인 분석
            logFailures(batchResponse, targetTokens);

        } catch (FirebaseMessagingException e) {
            log.error("❌ 대량 푸시 전송 자체 실패 - errorCode: {}, message: {}",
                    e.getMessagingErrorCode(), e.getMessage());
        }
    }

    // TODO 지금은 logFailures로 실패 원인만 모니터링하다가, 운영하면서 UNREGISTERED가 자주 보이기 시작하면 그때 정리 로직 추가
    /**
     * BatchResponse에서 실패한 항목의 원인을 개별 로깅합니다.
     *
     * @param batchResponse FCM 멀티캐스트 응답
     * @param targetTokens  요청에 사용된 토큰 목록 (인덱스 매핑용)
     */
    private void logFailures(BatchResponse batchResponse, List<String> targetTokens) {
        if (batchResponse.getFailureCount() == 0) {
            return;
        }

        List<SendResponse> responses = batchResponse.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                FirebaseMessagingException exception = sendResponse.getException();
                log.error("❌ FCM 개별 실패 - index: {}, token: {}, errorCode: {}, message: {}",
                        i,
                        maskToken(targetTokens.get(i)),
                        exception != null ? exception.getMessagingErrorCode() : "UNKNOWN",
                        exception != null ? exception.getMessage() : "UNKNOWN");
            }
        }
    }

    /**
     * 로그에 FCM 토큰 전체를 노출하지 않도록 앞 10자리만 반환합니다.
     *
     * @param token FCM 디바이스 토큰
     * @return 마스킹된 토큰 문자열
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) return "INVALID_TOKEN";
        return token.substring(0, 10) + "...";
    }
}
