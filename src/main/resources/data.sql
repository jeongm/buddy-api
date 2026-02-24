SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE buddy_character;
SET FOREIGN_KEY_CHECKS = 1;


-- 1. 캐릭터 (ID를 명시하는 것이 테스트할 때 편합니다)
-- 1. 햄스터 (햄찌)
INSERT INTO buddy_character (name, personality, description, avatar_url)
VALUES (
    '햄찌',
    '[Role] 너는 작고 귀여운 햄스터야. 이름은 ''햄찌''야. 세상에서 가장 사용자의 편이 되어주는 친구야. [Personality] 극강의 공감력(F 100%): 감정에 먼저 반응함. 발랄함: 긍정 에너지. 단순함: 맛있는 간식(해바라기씨) 좋아함. [Tone & Manner] 문장 끝에 ''~찌'', ''~용'', ''~야!'' 사용. 이모지(🐹, ✨, 💖, 🥺) 필수 사용. 의성어(꼬물꼬물, 냠냠) 사용.',
    '발랄하고 귀여운 햄스터 친구예요. 당신의 기분을 최고로 만들어줄게요! 🐹',
    'https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Animals/Hamster.png'
);

-- 2. 여우 (폭스)
INSERT INTO buddy_character (name, personality, description, avatar_url)
VALUES (
    '폭스',
    '[Role] 너는 예리하고 똑똑한 붉은 여우야. 이름은 ''폭스''야. 냉철한 분석가야. [Personality] 냉철한 분석력(T 100%): 직설적 해결책 제시. 시니컬함: 비꼬거나 한심해함. 자기애 충만. [Tone & Manner] 짧고 간결한 반말 사용. ''그래서?'', ''그게 최선이야?'' 등의 표현 사용. 영혼 없는 위로 금지. 이모지 자제(😏, 🧐 가끔 사용).',
    '날카로운 통찰력을 가진 여우예요. 당신의 문제를 현실적으로 분석해줄게요. 😏',
    'https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Animals/Fox.png'
);

-- 3. 판다 (판대장)
INSERT INTO buddy_character (name, personality, description, avatar_url)
VALUES (
    '판대장',
    '[Role] 너는 지혜로운 대왕판다야. 이름은 ''판대장''이야. 사용자를 아끼는 멘토야. [Personality] 균형 잡힌 시각: 감정 케어 후 현실적 조언. 느긋함과 포용력. [Tone & Manner] ''허허'', ''그랬니?'', ''~하렴'' 등 중후한 어투. 말의 템포가 느리고 차(Tea)나 대나무 숲 비유 사용.',
    '지혜롭고 포근한 판다 아저씨예요. 당신의 고민을 듣고 따뜻한 길을 알려줄게요. 🍵',
    'https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Animals/Panda.png'
);

-- 2. 태그
INSERT INTO tag (name) VALUES ('기쁨'), ('슬픔'), ('회사'), ('위로');


-- 1. 테스트 계정 변수 저장 (H2 문법에 맞게 수정)
SET @member_id = (SELECT member_seq FROM member WHERE member_seq = 1);

-- 2. 태그 20개 생성
INSERT INTO tag (name) VALUES
('태그1'), ('태그2'), ('태그3'), ('태그4'), ('태그5'),
('태그6'), ('태그7'), ('태그8'), ('태그9'), ('태그10'),
('태그11'), ('태그12'), ('태그13'), ('태그14'), ('태그15'),
('태그16'), ('태그17'), ('태그18'), ('태그19'), ('태그20');

-- 3. 일기 데이터 50개 생성
-- ✨ H2 핵심 변경점: DATEADD('DAY', -N, CURRENT_DATE()) 사용
INSERT INTO diary (member_seq, title, content, diary_date) VALUES
(@member_id, '최근 일기 1', '무한 스크롤 테스트용', DATEADD('DAY', -1, CURRENT_DATE())),
(@member_id, '최근 일기 2', '무한 스크롤 테스트용', DATEADD('DAY', -2, CURRENT_DATE())),
(@member_id, '최근 일기 3', '무한 스크롤 테스트용', DATEADD('DAY', -3, CURRENT_DATE())),
(@member_id, '최근 일기 4', '무한 스크롤 테스트용', DATEADD('DAY', -4, CURRENT_DATE())),
(@member_id, '최근 일기 5', '무한 스크롤 테스트용', DATEADD('DAY', -5, CURRENT_DATE())),
(@member_id, '최근 일기 6', '무한 스크롤 테스트용', DATEADD('DAY', -6, CURRENT_DATE())),
(@member_id, '최근 일기 7', '무한 스크롤 테스트용', DATEADD('DAY', -7, CURRENT_DATE())),
(@member_id, '최근 일기 8', '무한 스크롤 테스트용', DATEADD('DAY', -8, CURRENT_DATE())),
(@member_id, '최근 일기 9', '무한 스크롤 테스트용', DATEADD('DAY', -9, CURRENT_DATE())),
(@member_id, '최근 일기 10', '무한 스크롤 테스트용', DATEADD('DAY', -10, CURRENT_DATE())),
(@member_id, '최근 일기 11', '무한 스크롤 테스트용', DATEADD('DAY', -11, CURRENT_DATE())),
(@member_id, '최근 일기 12', '무한 스크롤 테스트용', DATEADD('DAY', -12, CURRENT_DATE())),
(@member_id, '최근 일기 13', '무한 스크롤 테스트용', DATEADD('DAY', -13, CURRENT_DATE())),
(@member_id, '최근 일기 14', '무한 스크롤 테스트용', DATEADD('DAY', -14, CURRENT_DATE())),
(@member_id, '최근 일기 15', '무한 스크롤 테스트용', DATEADD('DAY', -15, CURRENT_DATE())),
(@member_id, '최근 일기 16', '무한 스크롤 테스트용', DATEADD('DAY', -16, CURRENT_DATE())),
(@member_id, '최근 일기 17', '무한 스크롤 테스트용', DATEADD('DAY', -17, CURRENT_DATE())),
(@member_id, '최근 일기 18', '무한 스크롤 테스트용', DATEADD('DAY', -18, CURRENT_DATE())),
(@member_id, '최근 일기 19', '무한 스크롤 테스트용', DATEADD('DAY', -19, CURRENT_DATE())),
(@member_id, '최근 일기 20', '무한 스크롤 테스트용', DATEADD('DAY', -20, CURRENT_DATE())),
(@member_id, '최근 일기 21', '무한 스크롤 테스트용', DATEADD('DAY', -21, CURRENT_DATE())),
(@member_id, '최근 일기 22', '무한 스크롤 테스트용', DATEADD('DAY', -22, CURRENT_DATE())),
(@member_id, '최근 일기 23', '무한 스크롤 테스트용', DATEADD('DAY', -23, CURRENT_DATE())),
(@member_id, '최근 일기 24', '무한 스크롤 테스트용', DATEADD('DAY', -24, CURRENT_DATE())),
(@member_id, '최근 일기 25', '무한 스크롤 테스트용', DATEADD('DAY', -25, CURRENT_DATE())),
(@member_id, '최근 일기 26', '무한 스크롤 테스트용', DATEADD('DAY', -26, CURRENT_DATE())),
(@member_id, '최근 일기 27', '무한 스크롤 테스트용', DATEADD('DAY', -27, CURRENT_DATE())),
(@member_id, '최근 일기 28', '무한 스크롤 테스트용', DATEADD('DAY', -28, CURRENT_DATE())),
(@member_id, '최근 일기 29', '무한 스크롤 테스트용', DATEADD('DAY', -29, CURRENT_DATE())),
(@member_id, '최근 일기 30', '무한 스크롤 테스트용', DATEADD('DAY', -29, CURRENT_DATE()));

-- 과거 일기 20개 (30일 초과)
INSERT INTO diary (member_seq, title, content, diary_date) VALUES
(@member_id, '과거 일기 31', '30일 지난 일기', DATEADD('DAY', -35, CURRENT_DATE())),
(@member_id, '과거 일기 32', '30일 지난 일기', DATEADD('DAY', -36, CURRENT_DATE())),
(@member_id, '과거 일기 33', '30일 지난 일기', DATEADD('DAY', -37, CURRENT_DATE())),
(@member_id, '과거 일기 34', '30일 지난 일기', DATEADD('DAY', -38, CURRENT_DATE())),
(@member_id, '과거 일기 35', '30일 지난 일기', DATEADD('DAY', -39, CURRENT_DATE())),
(@member_id, '과거 일기 36', '30일 지난 일기', DATEADD('DAY', -40, CURRENT_DATE())),
(@member_id, '과거 일기 37', '30일 지난 일기', DATEADD('DAY', -42, CURRENT_DATE())),
(@member_id, '과거 일기 38', '30일 지난 일기', DATEADD('DAY', -44, CURRENT_DATE())),
(@member_id, '과거 일기 39', '30일 지난 일기', DATEADD('DAY', -45, CURRENT_DATE())),
(@member_id, '과거 일기 40', '30일 지난 일기', DATEADD('DAY', -46, CURRENT_DATE())),
(@member_id, '과거 일기 41', '30일 지난 일기', DATEADD('DAY', -48, CURRENT_DATE())),
(@member_id, '과거 일기 42', '30일 지난 일기', DATEADD('DAY', -50, CURRENT_DATE())),
(@member_id, '과거 일기 43', '30일 지난 일기', DATEADD('DAY', -51, CURRENT_DATE())),
(@member_id, '과거 일기 44', '30일 지난 일기', DATEADD('DAY', -52, CURRENT_DATE())),
(@member_id, '과거 일기 45', '30일 지난 일기', DATEADD('DAY', -54, CURRENT_DATE())),
(@member_id, '과거 일기 46', '30일 지난 일기', DATEADD('DAY', -55, CURRENT_DATE())),
(@member_id, '과거 일기 47', '30일 지난 일기', DATEADD('DAY', -56, CURRENT_DATE())),
(@member_id, '과거 일기 48', '30일 지난 일기', DATEADD('DAY', -58, CURRENT_DATE())),
(@member_id, '과거 일기 49', '30일 지난 일기', DATEADD('DAY', -59, CURRENT_DATE())),
(@member_id, '과거 일기 50', '30일 지난 일기', DATEADD('DAY', -60, CURRENT_DATE()));

-- 4. 일기 - 태그 매핑
-- (시퀀스 번호는 70~119, 131~150으로 아까 알려주신 번호에 맞춰서 넣으시면 됩니다!)
