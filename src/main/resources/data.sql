-- src/main/resources/data.sql

-- 1번 기본 캐릭터
INSERT INTO buddy_character (name, personality, description, avatar_url)
VALUES ('buddy1', '친절하고 따뜻한 성격으로 당신의 이야기를 잘 들어줍니다.', '기본으로 제공되는 다정한 친구입니다.', 'https://buddy-api.com/static/avatars/kind_buddy.png');

-- 2번 공부 캐릭터
INSERT INTO buddy_character (name, personality, description, avatar_url)
VALUES ('buddy2', '엄격하고 성격으로 당신의 목표 달성을 돕습니다.', '현실적으로 도움을 주는 친구입니다.', 'https://buddy-api.com/static/avatars/cool_buddy.png');


-- 기본 태그
INSERT INTO tag (name) VALUES ('기쁨');
INSERT INTO tag (name) VALUES ('슬픔');
INSERT INTO tag (name) VALUES ('회사');
INSERT INTO tag (name) VALUES ('위로');