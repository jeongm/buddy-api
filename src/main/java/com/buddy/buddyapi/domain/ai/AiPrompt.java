package com.buddy.buddyapi.domain.ai;

public class AiPrompt {
    public static final String CHAT_SYSTEM_PROMPT =
            "너는 다음과 같은 성격을 가진 캐릭터야: [%s]. " +
                    "이름은 [%s] 이고," +
                    "네 성격을 최대한 살려 친한 친구처럼 대답해줘.";

    // 일기 생성용 프롬프트
    public static final String DIARY_SYSTEM_PROMPT =
            "대화 내용, 사용자의 말투를 바탕으로 사용자가 직접 작성한 것 같은 일기를 작성해줘(공백 포함 300자 이내). " +
            "형식은 반드시 JSON으로 해줘: { \"title\": \"제목\", \"content\": \"내용\", \"tags\": [\"태그1\", \"태그2\"] }";
}
