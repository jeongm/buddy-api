-- =================================================================
-- V1__init_schema.sql
-- Flyway 최초 마이그레이션 - 전체 스키마 생성
-- DB : MariaDB / Encoding : utf8mb4
-- 주의 : 이 파일은 한 번 실행 후 절대 수정 금지 (Flyway 체크섬 검증)
--        수정이 필요하면 V2__ 파일을 새로 만들 것
-- =================================================================


-- -----------------------------------------------------------------
-- 1. buddy_character
--    캐릭터 마스터 테이블. Member 가 FK로 참조하므로 가장 먼저 생성.
-- -----------------------------------------------------------------
CREATE TABLE buddy_character
(
    character_id  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '캐릭터 PK',
    name          VARCHAR(100) NOT NULL                COMMENT '캐릭터 이름 (유니크)',
    personality   TEXT         NOT NULL                COMMENT '캐릭터 성격 (AI 시스템 프롬프트에 주입)',
    description   TEXT         NULL                    COMMENT '캐릭터 소개 문구 (앱 UI 표시용)',
    avatar_url    TEXT         NOT NULL DEFAULT '/'    COMMENT '캐릭터 아바타 이미지 URL',

    CONSTRAINT PK_buddy_character PRIMARY KEY (character_id),
    CONSTRAINT UX_buddy_character_name UNIQUE (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '버디 캐릭터 마스터 테이블';


-- -----------------------------------------------------------------
-- 2. member
--    회원 정보 테이블.
--    character_id 는 NULL 허용 (캐릭터 미선택 상태 또는 캐릭터 삭제 시 SET NULL)
-- -----------------------------------------------------------------
CREATE TABLE member
(
    member_id          BIGINT       NOT NULL AUTO_INCREMENT                COMMENT '회원 PK',
    email              VARCHAR(255) NOT NULL                               COMMENT '이메일 (유니크)',
    password           VARCHAR(255) NULL                                   COMMENT '비밀번호 해시 (소셜 전용 계정은 NULL)',
    nickname           VARCHAR(100) NOT NULL                               COMMENT '사용자 닉네임',
    joined_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '가입 일시',
    push_token         VARCHAR(255) NULL                                   COMMENT 'FCM 푸시 토큰',
    character_nickname VARCHAR(20)  NULL                                   COMMENT '사용자가 캐릭터에게 붙여준 별명',
    character_id       BIGINT       NULL                                   COMMENT '선택한 버디 캐릭터 FK',

    CONSTRAINT PK_member PRIMARY KEY (member_id),
    CONSTRAINT UX_member_email UNIQUE (email),
    CONSTRAINT FK_member_character
        FOREIGN KEY (character_id)
            REFERENCES buddy_character (character_id)
            ON DELETE SET NULL
            ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '회원 테이블';

-- 로그인/중복체크 시 email 조회 빈번 (unique 선언으로 이미 자동 생성되나 명시적 추가)
CREATE INDEX IX_member_email ON member (email);


-- -----------------------------------------------------------------
-- 3. oauth_account
--    소셜 로그인 연동 계정 (1회원 N소셜 가능)
--    provider + oauth_id 복합 유니크로 동일 소셜 중복 연동 방지
-- -----------------------------------------------------------------
CREATE TABLE oauth_account
(
    oauth_account_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '소셜 계정 PK',
    member_id            BIGINT       NOT NULL               COMMENT '회원 FK',
    provider             VARCHAR(10)  NOT NULL               COMMENT '소셜 제공자 (GOOGLE / NAVER / KAKAO)',
    oauth_id             VARCHAR(255) NOT NULL               COMMENT '소셜 제공자 고유 사용자 ID',
    social_access_token  TEXT         NULL                   COMMENT '소셜 액세스 토큰 (단기 만료)',
    social_refresh_token TEXT         NULL                   COMMENT '소셜 리프레시 토큰 (탈퇴 시 연동 해제용)',

    CONSTRAINT PK_oauth_account PRIMARY KEY (oauth_account_id),
    CONSTRAINT UX_oauth_provider_id UNIQUE (provider, oauth_id),
    CONSTRAINT FK_oauth_account_member
        FOREIGN KEY (member_id)
            REFERENCES member (member_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '소셜 로그인 연동 계정 테이블';

CREATE INDEX IX_oauth_account_member ON oauth_account (member_id);


-- -----------------------------------------------------------------
-- 4. notification_setting
--    회원별 알림 설정 (1:1)
-- -----------------------------------------------------------------
CREATE TABLE notification_setting
(
    setting_id         BIGINT  NOT NULL AUTO_INCREMENT COMMENT '알림 설정 PK',
    member_id          BIGINT  NOT NULL               COMMENT '회원 FK (유니크)',
    chat_alert_yn      BOOLEAN NOT NULL DEFAULT TRUE  COMMENT '대화 소멸 경고 알림 (10시간 경과 시)',
    daily_alert_yn     BOOLEAN NOT NULL DEFAULT FALSE COMMENT '데일리 안부 알림',
    marketing_alert_yn BOOLEAN NOT NULL DEFAULT FALSE COMMENT '마케팅/이벤트 알림',
    night_alert_yn     BOOLEAN NOT NULL DEFAULT FALSE COMMENT '야간 알림 수신 동의 (밤 9시~아침 8시)',

    CONSTRAINT PK_notification_setting PRIMARY KEY (setting_id),
    CONSTRAINT UX_notification_setting_member UNIQUE (member_id),
    CONSTRAINT FK_notification_setting_member
        FOREIGN KEY (member_id)
            REFERENCES member (member_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '회원별 알림 설정 테이블';


-- -----------------------------------------------------------------
-- 5. member_insight
--    회원 주간 인사이트 (1:1)
--    매주 갱신. 이전 주 일기가 없으면 NULL
-- -----------------------------------------------------------------
CREATE TABLE member_insight
(
    insight_id      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '인사이트 PK',
    member_id       BIGINT      NOT NULL               COMMENT '회원 FK (유니크)',
    weekly_identity VARCHAR(50) NULL                   COMMENT '주간 아이덴티티 칭호',
    weekly_keyword  VARCHAR(20) NULL                   COMMENT '주간 핵심 키워드 (단일 명사)',
    updated_at      DATETIME    NULL                   COMMENT '마지막 인사이트 갱신 일시',

    CONSTRAINT PK_member_insight PRIMARY KEY (insight_id),
    CONSTRAINT UX_member_insight_member UNIQUE (member_id),
    CONSTRAINT FK_member_insight_member
        FOREIGN KEY (member_id)
            REFERENCES member (member_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '회원 주간 인사이트 테이블';


-- -----------------------------------------------------------------
-- 6. chat_session
--    사용자 + 캐릭터 간 1회 대화 세션 단위
--    character_id : RESTRICT (세션이 있는 캐릭터는 삭제 불가)
-- -----------------------------------------------------------------
CREATE TABLE chat_session
(
    session_id           BIGINT   NOT NULL AUTO_INCREMENT          COMMENT '채팅 세션 PK',
    member_id            BIGINT   NOT NULL                         COMMENT '회원 FK',
    character_id         BIGINT   NOT NULL                         COMMENT '버디 캐릭터 FK',
    is_ended             BOOLEAN  NOT NULL DEFAULT FALSE           COMMENT '세션 종료 여부',
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '세션 생성 일시',
    deletion_notified_at DATETIME NULL                             COMMENT '소멸 경고 알림 발송 일시 (NULL = 미발송)',

    CONSTRAINT PK_chat_session PRIMARY KEY (session_id),
    CONSTRAINT FK_chat_session_member
        FOREIGN KEY (member_id)
            REFERENCES member (member_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,
    CONSTRAINT FK_chat_session_character
        FOREIGN KEY (character_id)
            REFERENCES buddy_character (character_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '채팅 세션 테이블';

-- 회원별 세션 목록 + 진행 중인 세션 필터링
CREATE INDEX IX_chat_session_member        ON chat_session (member_id);
CREATE INDEX IX_chat_session_member_ended  ON chat_session (member_id, is_ended);


-- -----------------------------------------------------------------
-- 7. chat_message
--    채팅 메시지. 세션 삭제 시 CASCADE 삭제.
-- -----------------------------------------------------------------
CREATE TABLE chat_message
(
    message_id BIGINT      NOT NULL AUTO_INCREMENT            COMMENT '메시지 PK',
    session_id BIGINT      NOT NULL                           COMMENT '채팅 세션 FK',
    role       VARCHAR(20) NOT NULL                           COMMENT '발화 주체 (USER / ASSISTANT)',
    content    TEXT        NOT NULL                           COMMENT '메시지 본문',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '메시지 생성 일시',

    CONSTRAINT PK_chat_message PRIMARY KEY (message_id),
    CONSTRAINT FK_chat_message_session
        FOREIGN KEY (session_id)
            REFERENCES chat_session (session_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,
    CONSTRAINT CK_chat_message_role CHECK (role IN ('USER', 'ASSISTANT'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '채팅 메시지 테이블';

-- 세션 내 메시지 시간순 조회
CREATE INDEX IX_chat_message_session ON chat_message (session_id, created_at);


-- -----------------------------------------------------------------
-- 8. tag
--    일기 태그 마스터. name 유니크로 중복 태그 방지.
-- -----------------------------------------------------------------
CREATE TABLE tag
(
    tag_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '태그 PK',
    name   VARCHAR(20) NOT NULL               COMMENT '태그명 (유니크)',

    CONSTRAINT PK_tag PRIMARY KEY (tag_id),
    CONSTRAINT UX_tag_name UNIQUE (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '일기 태그 마스터 테이블';


-- -----------------------------------------------------------------
-- 9. diary
--    일기 테이블.
--    session_id : SET NULL (세션 삭제돼도 일기는 보존)
--    직접 작성 일기는 session_id = NULL
-- -----------------------------------------------------------------
CREATE TABLE diary
(
    diary_id   BIGINT       NOT NULL AUTO_INCREMENT            COMMENT '일기 PK',
    member_id  BIGINT       NOT NULL                           COMMENT '회원 FK',
    session_id BIGINT       NULL                               COMMENT '연결된 채팅 세션 FK (직접 작성 시 NULL)',
    title      VARCHAR(100) NULL                               COMMENT '일기 제목',
    content    TEXT         NULL                               COMMENT '일기 본문',
    diary_date DATE         NOT NULL                           COMMENT '일기 날짜 (사용자 로컬 기준)',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '레코드 생성 일시',
    image_url  TEXT         NULL                               COMMENT '첨부 이미지 URL (Cloudinary)',

    CONSTRAINT PK_diary PRIMARY KEY (diary_id),
    CONSTRAINT UX_diary_session UNIQUE (session_id),
    CONSTRAINT FK_diary_member
        FOREIGN KEY (member_id)
            REFERENCES member (member_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,
    CONSTRAINT FK_diary_session
        FOREIGN KEY (session_id)
            REFERENCES chat_session (session_id)
            ON DELETE SET NULL
            ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '일기 테이블';

-- 회원별 일기 목록 조회 + 달력 뷰 날짜 정렬
CREATE INDEX IX_diary_member      ON diary (member_id);
CREATE INDEX IX_diary_member_date ON diary (member_id, diary_date DESC);


-- -----------------------------------------------------------------
-- 10. diary_tag
--     일기-태그 N:M 중간 테이블. 복합 PK.
--     diary 삭제 시 CASCADE / tag 삭제는 RESTRICT
-- -----------------------------------------------------------------
CREATE TABLE diary_tag
(
    diary_id BIGINT NOT NULL COMMENT '일기 FK (복합 PK)',
    tag_id   BIGINT NOT NULL COMMENT '태그 FK (복합 PK)',

    CONSTRAINT PK_diary_tag PRIMARY KEY (diary_id, tag_id),
    CONSTRAINT FK_diary_tag_diary
        FOREIGN KEY (diary_id)
            REFERENCES diary (diary_id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,
    CONSTRAINT FK_diary_tag_tag
        FOREIGN KEY (tag_id)
            REFERENCES tag (tag_id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '일기-태그 N:M 연관 테이블';

-- 특정 태그가 달린 일기 역조회
CREATE INDEX IX_diary_tag_tag ON diary_tag (tag_id);
