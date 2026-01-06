SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE buddy_character;
SET FOREIGN_KEY_CHECKS = 1;


-- 1. 캐릭터 (ID를 명시하는 것이 테스트할 때 편합니다)
INSERT INTO buddy_character ( name, personality, description, avatar_url)
VALUES ('buddy1', '친절하고 따뜻한 성격', '다정한 친구', 'http://image.com/1');

INSERT INTO buddy_character ( name, personality, description, avatar_url)
VALUES ('buddy2', '엄격한 성격', '공부 친구', 'http://image.com/2');

-- 2. 태그
INSERT INTO tag (name) VALUES ('기쁨'), ('슬픔'), ('회사'), ('위로');

-- 3. 멤버 (이메일 형식을 제대로 갖추고, character_seq 대신 character_id 등 컬럼명을 확인하세요)
INSERT INTO member (email, nickname, password, character_seq, joined_at)
VALUES ('test1@test.com', 'test', '1', 1, NOW());