-- =================================================================
-- V3__insert_test_data.sql
-- 개발/테스트용 더미 데이터 삽입
-- ⚠️ 운영 환경에서는 절대 실행되면 안됨
--    application-prod.yml → flyway.target: 2 로 설정하여 차단
-- =================================================================

-- 테스트용 회원 (비밀번호: 1234 bcrypt 해시)
INSERT INTO member (email, password, nickname, joined_at)
VALUES (
    'je0ng22@naver.com',
    '$2a$12$5zBPA6ydRXLdiomoB.1zRO/rjKLGLqleHk9rGOw9.kJR5b7olzlzW',
    '테스트계정',
    CURRENT_TIMESTAMP
);

INSERT INTO member (email, password, nickname, joined_at)
VALUES (
    'buddyzzang11@gmail.com',
    '$2a$12$OdAHUOyF6NgndLY21yiz.u8wqlcTDuNqv/08O9dfjymXnLagxJZCW',
    '버디짱',
    CURRENT_TIMESTAMP
);

-- 테스트 회원 알림 설정 (없으면 조회/탈퇴 시 에러)
INSERT INTO notification_setting (member_id, chat_alert_yn, daily_alert_yn, marketing_alert_yn, night_alert_yn)
VALUES (1, true, false, false, true);
INSERT INTO notification_setting (member_id, chat_alert_yn, daily_alert_yn, marketing_alert_yn, night_alert_yn)
VALUES (2, true, true, true, true);
