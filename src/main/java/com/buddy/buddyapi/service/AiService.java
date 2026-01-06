package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.request.OpenAiRequest;
import com.buddy.buddyapi.dto.response.OpenAiResponse;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Getter
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public String getChatResponse(String conversation, String characterPersonality) {
        // 1. 프롬프트 구성 (AI에게 부여할 페르소나와 지시문)
        String prompt = String.format(
                "너는 다음과 같은 성격을 가진 캐릭터야: [%s]. " +
                        "성격에 맞춰서 대답해줘 " +
                        "형식은 반드시 JSON으로 해줘: { \"content\": \"내용\" }",
                characterPersonality
        );

        // 2. 요청 객체 생성
        OpenAiRequest request = new OpenAiRequest(
                "gpt-3.5-turbo",
                List.of(
                        new OpenAiRequest.Message("system", prompt),
                        new OpenAiRequest.Message("user", conversation)
                ),
                0.7
        );

        // 3. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<OpenAiRequest> entity = new HttpEntity<>(request, headers);

        // 4. 호출 및 응답 받기
        try {
            OpenAiResponse response = restTemplate.postForObject(apiUrl, entity, OpenAiResponse.class);

            if (response == null || response.choices().isEmpty()) {
                throw new BaseException(ResultCode.AI_PARSE_ERROR);
            }

            // OpenAI가 준 JSON 문자열(content)만 반환
            return response.choices().get(0).message().content();

        } catch (Exception e) {
            log.error("OpenAI 호출 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.AI_PARSE_ERROR);
        }
    }

    public String getDiaryDraft(String conversation) {
        // 1. 프롬프트 구성 (AI에게 부여할 페르소나와 지시문)
        String prompt = String.format(
                        "대화 내용을 바탕으로 사용자가 직접 작성한 것 같은 일기를 작성해줘. " +
                        "형식은 반드시 JSON으로 해줘: { \"title\": \"제목\", \"content\": \"내용\", \"tags\": [\"태그1\", \"태그2\"] }"
        );

        // 2. 요청 객체 생성
        OpenAiRequest request = new OpenAiRequest(
                "gpt-3.5-turbo",
                List.of(
                    new OpenAiRequest.Message("system", prompt),
                    new OpenAiRequest.Message("user", conversation)
                ),
                0.7
        );

        // 3. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<OpenAiRequest> entity = new HttpEntity<>(request, headers);

        // 4. 호출 및 응답 받기
        try {
            OpenAiResponse response = restTemplate.postForObject(apiUrl, entity, OpenAiResponse.class);

            if (response == null || response.choices().isEmpty()) {
                throw new BaseException(ResultCode.AI_PARSE_ERROR);
            }

            log.info("{}", response.choices().get(0).message().content());
            // OpenAI가 준 JSON 문자열(content)만 반환
            return response.choices().get(0).message().content();

        } catch (Exception e) {
            log.error("OpenAI 호출 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.AI_PARSE_ERROR);
        }

    }
}
