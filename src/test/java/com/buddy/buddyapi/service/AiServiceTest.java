package com.buddy.buddyapi.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiServiceTest {

    @Autowired
    AiService aiService;

    @Test
    void openAIAPIValueTest() {
        // URL 확인
        Assertions.assertThat(aiService.getApiUrl())
                .isEqualTo("https://api.openai.com/v1/chat/completions");

        // Key 확인 (s가 나오는지, 실제 키가 나오는지, 아니면 DEFAULT_KEY가 나오는지)
        System.out.println("테스트에서 읽어온 키: " + aiService.getApiKey());
    }

}