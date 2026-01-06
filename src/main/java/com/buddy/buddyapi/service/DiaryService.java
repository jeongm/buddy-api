package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.request.DiaryCreateRequest;
import com.buddy.buddyapi.dto.request.DiaryGenerateRequest;
import com.buddy.buddyapi.dto.request.DiaryUpdateRequest;
import com.buddy.buddyapi.dto.response.DiaryDetailResponse;
import com.buddy.buddyapi.dto.response.DiaryListResponse;
import com.buddy.buddyapi.dto.response.DiaryPreviewResponse;
import com.buddy.buddyapi.dto.response.TagResponse;
import com.buddy.buddyapi.entity.*;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.repository.ChatMessageRepository;
import com.buddy.buddyapi.repository.ChatSessionRepository;
import com.buddy.buddyapi.repository.DiaryRepository;
import com.buddy.buddyapi.repository.TagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final TagRepository tagRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public DiaryPreviewResponse generateDiaryFromChat(Member member, DiaryGenerateRequest request) throws JsonProcessingException {
        // 1. 세션 조회 (내 세션인지, 종료된 세션인지 확인)
        ChatSession session = chatSessionRepository.findBySessionSeqAndMember(request.sessionId(), member)
                .orElseThrow(() -> new BaseException(ResultCode.SESSION_NOT_FOUND));

        // 2. 해당 세션의 모든 메시지 시간순 조회
        List<ChatMessage> messages = chatMessageRepository.findAllByChatSessionOrderByCreatedAtAsc(session);

        if (messages.isEmpty()) {
            throw new BaseException(ResultCode.EMPTY_CHAT_HISTORY); // 대화가 없으면 일기 생성 불가
        }

        // 3. AI에게 전달할 대화 텍스트 포맷팅
        // 예: "USER: 오늘 힘들어 / ASSISTANT: 무슨 일이야? / USER: 상사한테 깨졌어"
        String fullConversation = messages.stream()
                .map(m -> String.format("%s: %s", m.getRole(), m.getContent()))
                .collect(Collectors.joining("\n"));

        // 4. AI 서비스 호출 (페르소나와 대화 내용 전달)
        log.info("AI에게 보낼 텍스트:\n{}", fullConversation);

        String rawResponse = aiService.getDiaryDraft(
                fullConversation
        );

        log.info("일기 작성 : \n{}",rawResponse);

        // 5. AI 응답(JSON 문자열)을 DTO로 변환 (파싱 로직은 아래에서 구현)
        return parseAiResponse(rawResponse);
    }

    private DiaryPreviewResponse parseAiResponse(String jsonString) {
        try {
            // AI가 가끔 ```json ... ``` 이런 식으로 답을 줄 때를 대비해 앞뒤 정리
            String cleanedJson = jsonString.substring(jsonString.indexOf("{"), jsonString.lastIndexOf("}") + 1);

            // AI가 주는 JSON 필드에 맞춰 임시 클래스로 먼저 받거나 직접 매핑
            JsonNode root = objectMapper.readTree(cleanedJson);

            String title = root.path("title").asText();
            String content = root.path("content").asText();

            // 태그는 우선 이름(String) 리스트로 받아옵니다.
            List<String> tagNames = new ArrayList<>();
            root.path("tags").forEach(t -> tagNames.add(t.asText()));

            List<TagResponse> tags = tagNames.stream()
                    .map(name -> {
                        // DB에 태그가 있으면 가져오고, 없으면 새로 생성(Optional 활용)
                        Tag tag = tagRepository.findByName(name)
                                .orElseGet(() -> tagRepository.save(new Tag(name)));
                        return new TagResponse(tag.getTagSeq(), tag.getName());
                    })
                    .toList();

            return new DiaryPreviewResponse(title, content, tags);

        } catch (Exception e) {
            // 파싱 실패 시 기본 응답을 주거나 예외 처리
            log.error("AI 응답 파싱 에러. 원본 데이터: {}", jsonString, e);
            throw new BaseException(ResultCode.AI_PARSE_ERROR);
        }
    }

    // 특정 날짜의 일기 목록 조회
    public List<DiaryListResponse> getDiariesByDate(Member member, LocalDate date) {
        // 해당 날짜의 00:00:00
        LocalDateTime start = date.atStartOfDay();
        // 해당 날짜의 23:59:59.999999
        LocalDateTime end = date.atTime(LocalTime.MAX);

        return diaryRepository.findAllByMemberAndCreatedAtBetweenOrderByCreatedAtDesc(member, start, end)
                .stream()
                .map(DiaryListResponse::from) // DTO에 만든 from 메서드 활용
                .toList();
    }

    // 일기 저장 (Create)
    @Transactional
    public Long createDiary(Member member, DiaryCreateRequest request) {
        // 1. 일기 엔티티 생성
        Diary diary = Diary.builder()
                .title(request.title())
                .content(request.content())
                .imageUrl(request.imageUrl())
                .member(member)
                .build();

        // 2. 태그 리스트가 있다면 조회 후 연결
        if (request.tagSeqs() != null && !request.tagSeqs().isEmpty()) {
            List<Tag> tags = tagRepository.findAllByTagSeqIn(request.tagSeqs());

            // 요청한 개수와 DB에서 찾은 개수가 다르면 예외 처리 (선택사항)
            if (tags.size() != request.tagSeqs().size()) {
                throw new BaseException(ResultCode.TAG_NOT_FOUND);
            }

            diary.addTags(tags);
        }

        return diaryRepository.save(diary).getDiarySeq();
    }

    @Transactional
    public void updateDiary(Member member, Long diarySeq, DiaryUpdateRequest request) {
        // 1. 본인의 일기인지 확인하며 조회
        Diary diary = diaryRepository.findByDiarySeqAndMember(diarySeq, member)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        // 2. 기본 정보 수정
        diary.updateDiary(request.title(), request.content(), request.imageUrl());

        // 3. 태그 교체
        if (request.tagSeqs() != null) {
            List<Tag> tags = tagRepository.findAllById(request.tagSeqs());
            diary.updateTags(tags);
        }
    }

    @Transactional
    public void deleteDiary(Member member, Long diarySeq) {
        Diary diary = diaryRepository.findByDiarySeqAndMember(diarySeq, member)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        diaryRepository.delete(diary);
        // diary_tag 테이블의 데이터도 알아서 같이 지워집니다!
    }

    @Transactional(readOnly = true)
    public DiaryDetailResponse getDiaryDetail(Member member, Long diarySeq) {
        Diary diary = diaryRepository.findByDiarySeqAndMember(diarySeq, member)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        return DiaryDetailResponse.from(diary);
    }

}
