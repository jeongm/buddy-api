package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.request.OpenAiRequest;
import com.buddy.buddyapi.dto.response.OpenAiResponse;
import com.buddy.buddyapi.global.aspect.Timer;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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
public class AiService {

    private final String apiKey;
    private final String apiUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.api.url}") String apiUrl,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 채팅 시 openai 프롬프트 및 호출
     * @param messages 조립이 완료된 전체 메시지 리스트 (System + History + User)
     * @return AI가 생성한 응답 문자열
     */
    @Timer
    public String getChatResponse(List<OpenAiRequest.Message> messages) {
        return callOpenAi(messages,false);
    }

    /**
     * AI를 이용한 일기 작성 시 openai 호출
     * @param conversations 해당 세션의 전체 대화 내역
     * @return AI가 생성한 일기 초안 문자열
     */
    @Timer
    public String getDiaryDraft(String conversations) {
        String systemMessage = String.format(
                AiPrompt.DIARY_SYSTEM_PROMPT
        );

        List<OpenAiRequest.Message> messages = List.of(
          new OpenAiRequest.Message("system", systemMessage),
          new OpenAiRequest.Message("user", conversations)
        );

        return callOpenAi(messages, true);
    }

    /**
     * openai 호출
     * @param messages prompt AI에게 전달할 시스템 지시문
     * @param isJsonRequest 응답 형식이 JSON이어야 하는지 여부
     * @return 정제된 AI 응답 문자열
     */
    private String callOpenAi(List<OpenAiRequest.Message> messages, boolean isJsonRequest) {
        // 요청 객체 생성
        OpenAiRequest request = new OpenAiRequest(
                "gpt-3.5-turbo",
                messages,
                0.7
        );

        HttpEntity<OpenAiRequest> entity = new HttpEntity<>(request, createHeaders());

        // 호출 및 응답 받기
        try {
            OpenAiResponse response = restTemplate.postForObject(apiUrl, entity, OpenAiResponse.class);

            if (response == null || response.choices().isEmpty()) {
                throw new BaseException(ResultCode.AI_PARSE_ERROR);
            }

            // OpenAI가 준 JSON 문자열(content)만 반환
            String content = response.choices().get(0).message().content();
            log.info("openai 응답 : {}", content);

            // 채팅 요청인데 JSON 형식({ "content": "..." })으로 왔을 때 텍스트만 추출
            if (!isJsonRequest && content.trim().startsWith("{")) {
                try {
                    JsonNode node = objectMapper.readTree(content);
                    // 만약 내부 필드명이 "content"인 JSON이라면 그 값만 가져옴
                    if (node.has("content")) {
                        return node.get("content").asText();
                    }
                } catch (Exception e) {
                    log.warn("응답이 JSON처럼 보이지만 파싱할 수 없습니다. 원문 그대로 반환합니다.");
                }
            }


            return content;
        } catch (Exception e) {
            log.error("OpenAI 호출 실패: {}", e.getMessage());
            throw new BaseException(ResultCode.AI_PARSE_ERROR);
        }

    }

    /**
     * 헤더 생성
     * @return 공통 헤더 규격
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        return headers;
    }
}
