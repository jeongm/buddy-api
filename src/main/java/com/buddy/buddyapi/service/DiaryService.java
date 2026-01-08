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

    /**
     * 채팅 내역을 기반으로 AI 일기 초안을 생성합니다. (DB 저장 안 함)
     *
     * @param member  현재 로그인한 회원 정보
     * @param request 일기 생성을 위한 세션 ID가 포함된 요청 DTO
     * @return AI가 생성한 일기 제목, 본문, 추천 태그 정보를 담은 프리뷰 응답 DTO
     * @throws BaseException 세션을 찾을 수 없거나 대화 내역이 비어있을 경우 발생
     */
    public DiaryPreviewResponse generateDiaryFromChat(Member member, DiaryGenerateRequest request) {
        // 1. 세션 조회 (내 세션인지, 종료된 세션인지 확인)
        ChatSession session = chatSessionRepository.findBySessionSeqAndMember(request.sessionId(), member)
                .orElseThrow(() -> new BaseException(ResultCode.SESSION_NOT_FOUND));

        // 2. 해당 세션의 모든 메시지 시간순 조회
        List<ChatMessage> messages = chatMessageRepository.findAllByChatSessionOrderByCreatedAtAsc(session);

        if (messages.isEmpty()) {
            throw new BaseException(ResultCode.EMPTY_CHAT_HISTORY); // 대화가 없으면 일기 생성 불가
        }

        // 3. AI에게 전달할 대화 텍스트 포맷팅
        // 예: "USER: 오늘 힘들어 / ASSISTANT: 무슨 일이야?
        String fullConversation = messages.stream()
                .map(m -> String.format("%s: %s", m.getRole(), m.getContent()))
                .collect(Collectors.joining("\n"));

        // 4. AI 서비스 호출 (페르소나와 대화 내용 전달)
        log.info("AI에게 보낼 텍스트:\n{}", fullConversation);

        String rawResponse = aiService.getDiaryDraft(
                fullConversation
        );

        log.info("AI 응답 일기 초안 : \n{}",rawResponse);

        // 5. AI 응답(JSON 문자열)을 DTO로 변환 (파싱 로직은 아래에서 구현)
        return parseAiResponse(rawResponse);
    }

    /**
     * AI의 JSON 응답 문자열을 파싱하여 객체로 변환합니다.
     *
     * @param jsonString AI 서비스로부터 받은 JSON 포맷의 문자열
     * @return 파싱된 일기 데이터와 태그 리스트가 포함된 DTO
     * @throws BaseException JSON 파싱에 실패하거나 규격이 맞지 않을 경우 발생
     */
    private DiaryPreviewResponse parseAiResponse(String jsonString) {
        try {

            String cleanedJson = jsonString.substring(jsonString.indexOf("{"), jsonString.lastIndexOf("}") + 1);

            // AI가 주는 JSON 필드에 맞춰 임시 클래스로 먼저 받거나 직접 매핑
            JsonNode root = objectMapper.readTree(cleanedJson);

            String title = root.path("title").asText();
            String content = root.path("content").asText();

            // 태그 또한 우선 프리 뷰 단계이므로 저장하지 않습니다.
            List<TagResponse> tags = new ArrayList<>();
            root.path("tags").forEach(t -> {
                String name = t.asText();

                Tag tag = tagRepository.findByName(name)
                        .orElseGet(() -> new Tag(name));
                tags.add(new TagResponse(tag.getTagSeq(), tag.getName()));
            });

            return new DiaryPreviewResponse(title, content, tags);

        } catch (Exception e) {
            // 파싱 실패 시 기본 응답을 주거나 예외 처리
            log.error("AI 응답 파싱 에러. 원본 데이터: {}", jsonString, e);
            throw new BaseException(ResultCode.AI_PARSE_ERROR);
        }
    }

    /**
     * 특정 날짜에 작성된 일기 목록을 조회합니다.
     *
     * @param member 현재 로그인한 회원 정보
     * @param date   조회하고자 하는 날짜 (yyyy-MM-dd)
     * @return 해당 날짜에 작성된 일기 리스트 (최신순)
     */
    public List<DiaryListResponse> getDiariesByDate(Member member, LocalDate date) {
        // 해당 날짜의 00:00:00 ~ 23:59:59.999999
        // 인덱스 활용을 위해 범위를 직접 지정 (Index Range Scan 유도)
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        return diaryRepository.findAllByMemberAndCreatedAtBetweenOrderByCreatedAtDesc(member, start, end)
                .stream()
                .map(DiaryListResponse::from) // DTO에 만든 from 메서드 활용
                .toList();
    }

    /**
     * 사용자가 최종 확정한 일기 데이터를 DB에 저장합니다.
     *
     * @param member  현재 로그인한 회원 정보
     * @param request 저장할 일기 제목, 내용, 이미지, 태그 ID 리스트 등을 담은 DTO
     * @return 생성된 일기의 고유 식별자 (ID)
     * @throws BaseException 요청한 태그 ID가 존재하지 않을 경우 발생
     */
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
        if (request.tags() != null && !request.tags().isEmpty()) {
            List<Tag> tags = getOrCreateTags(request.tags());
            diary.addTags(tags);
        }

        return diaryRepository.save(diary).getDiarySeq();
    }

    /**
     * 태그 이름 리스트를 바탕으로 기존 태그를 조회하거나 신규 태그를 생성합니다.
     */
    private List<Tag> getOrCreateTags(List<String> tagNames) {
        return tagNames.stream()
                .map(name -> tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(new Tag(name))))
                .collect(Collectors.toList());
    }

    /**
     * 기존에 작성된 일기 내용을 수정합니다.
     *
     * @param member   현재 로그인한 회원 정보
     * @param diarySeq 수정할 일기의 고유 식별자
     * @param request  수정할 제목, 내용, 이미지, 태그 리스트 등을 담은 DTO
     * @throws BaseException 일기를 찾을 수 없거나 본인 일기가 아닐 경우 발생
     */
    @Transactional
    public void updateDiary(Member member, Long diarySeq, DiaryUpdateRequest request) {
        // 1. 본인의 일기인지 확인하며 조회
        Diary diary = diaryRepository.findByDiarySeqAndMember(diarySeq, member)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        // 2. 기본 정보 수정
        diary.updateDiary(request.title(), request.content(), request.imageUrl());

        // 3. 태그 교체
        if (request.tags() != null) {
            List<Tag> tags = getOrCreateTags(request.tags());
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

    /**
     * 일기의 상세 내용을 조회합니다.
     *
     * @param member   현재 로그인한 회원 정보
     * @param diarySeq 조회할 일기의 고유 식별자
     * @return 일기 상세 정보 및 연관된 태그 정보를 포함한 DTO
     * @throws BaseException 일기를 찾을 수 없거나 본인 일기가 아닐 경우 발생
     */
    @Transactional(readOnly = true)
    public DiaryDetailResponse getDiaryDetail(Member member, Long diarySeq) {
        Diary diary = diaryRepository.findByDiarySeqAndMember(diarySeq, member)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        return DiaryDetailResponse.from(diary);
    }

}
