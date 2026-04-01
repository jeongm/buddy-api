-- =================================================================
-- V3__add_streak_columns_to_member_insight.sql
-- member_insight 테이블에 연속 기록 관련 컬럼 추가
-- 주의 : 이 파일은 한 번 실행 후 절대 수정 금지 (Flyway 체크섬 검증)
--        수정이 필요하면 V4__ 파일을 새로 만들 것
-- =================================================================


-- -----------------------------------------------------------------
-- member_insight
--    current_streak   : 현재 연속 기록 일수
--    best_streak      : 역대 최고 연속 기록 일수
--    last_diary_date : 마지막 일기 작성 날짜 (streak 끊김 여부 판단용)
--    updated_at -> weekly_updated_at 컬럼 이름 변경
-- -----------------------------------------------------------------
ALTER TABLE member_insight
    ADD COLUMN current_streak    INT  NOT NULL DEFAULT 0    COMMENT '현재 연속 기록 일수';
ALTER TABLE member_insight
    ADD COLUMN best_streak       INT  NOT NULL DEFAULT 0    COMMENT '역대 최고 연속 기록 일수';
ALTER TABLE member_insight
    ADD COLUMN last_diary_date DATE NULL                 COMMENT '마지막 일기 작성 날짜';

ALTER TABLE member_insight
    RENAME COLUMN updated_at TO weekly_updated_at;