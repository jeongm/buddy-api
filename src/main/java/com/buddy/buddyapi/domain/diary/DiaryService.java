package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.domain.chat.ChatMessage;
import com.buddy.buddyapi.domain.chat.ChatMessageRepository;
import com.buddy.buddyapi.domain.chat.ChatSession;
import com.buddy.buddyapi.domain.chat.ChatSessionRepository;
import com.buddy.buddyapi.domain.diary.dto.*;
import com.buddy.buddyapi.domain.member.Member;
import com.buddy.buddyapi.domain.member.MemberRepository;
import com.buddy.buddyapi.global.aspect.Timer;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.domain.ai.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final TagRepository tagRepository;
    private final MemberRepository memberRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final ImageService imageService;

    @Transactional(readOnly = true)
    public Slice<DiaryListResponse> getDiaryList(Long memberSeq, String search, Pageable pageable) {

        Slice<Diary> diarySlice = diaryRepository.searchMyDiaries(memberSeq, search, pageable);

        // 엔티티(Diary)를 DTO(DiaryListResponse)로 변환해서 반환
        return diarySlice.map(DiaryListResponse::from);
    }

    /**
     * 채팅 내역을 기반으로 AI 일기 초안을 생성합니다. (DB 저장 안 함)
     *
     * @param memberSeq  현재 로그인한 회원 정보
     * @param request 일기 생성을 위한 세션 ID가 포함된 요청 DTO
     * @return AI가 생성한 일기 제목, 본문, 추천 태그 정보를 담은 프리뷰 응답 DTO
     * @throws BaseException 세션을 찾을 수 없거나 대화 내역이 비어있을 경우 발생
     */
    @Timer
    @Transactional(readOnly = true)
    public DiaryPreviewResponse generateDiaryFromChat(Long memberSeq, DiaryGenerateRequest request) {

        // 1. 세션 조회 (내 세션인지, 종료된 세션인지 확인)
        ChatSession session = chatSessionRepository.findBySessionSeqAndMember_MemberSeq(request.sessionId(), memberSeq)
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
     * @param memberSeq 현재 로그인한 회원 정보
     * @param date   조회하고자 하는 날짜 (yyyy-MM-dd)
     * @return 해당 날짜에 작성된 일기 리스트 (최신순)
     */
    @Transactional(readOnly = true)
    public List<DiaryListResponse> getDiariesByDate(Long memberSeq, LocalDate date) {

        return diaryRepository.findAllByMemberAndDiaryDate(memberSeq, date)
                .stream()
                .map(DiaryListResponse::from)
                .toList();
    }

    /**
     * 사용자가 최종 확정한 일기 데이터를 DB에 저장합니다.
     *
     * @param memberSeq  현재 로그인한 회원 정보
     * @param request 저장할 일기 제목, 내용, 이미지, 태그 ID 리스트 등을 담은 DTO
     * @return 생성된 일기의 고유 식별자 (ID)
     * @throws BaseException 요청한 태그 ID가 존재하지 않을 경우 발생
     */
    @Transactional
    public Long createDiary(Long memberSeq, DiaryCreateRequest request, MultipartFile image) {

        Member member = memberRepository.findByIdOrThrow(memberSeq);

        ChatSession chatSession = null;
        if(request.sessionSeq() != null) {
            chatSession = chatSessionRepository.findBySessionSeqAndMember_MemberSeq(request.sessionSeq(), memberSeq)
                    .orElseThrow(() -> new BaseException(ResultCode.SESSION_NOT_FOUND));
        }

        // 이미지 파일이 있으면 저장하고 경로 반환받기
        String savedImageUrl = null;
        if(image != null && !image.isEmpty()) {
            savedImageUrl = imageService.uploadImage(image);
        }

        Diary diary = Diary.builder()
                .title(request.title())
                .content(request.content())
                .diaryDate(request.diaryDate())
                .imageUrl(savedImageUrl)
                .member(member)
                .chatSession(chatSession)
                .build();

        // 2. 태그 리스트가 있다면 조회 후 연결
        if (request.tags() != null && !request.tags().isEmpty()) {
            List<Tag> tags = getOrCreateTags(request.tags());
            diary.addTags(tags);
        }

        return diaryRepository.save(diary).getDiarySeq();
    }

    /**
     * 기존에 작성된 일기 내용을 수정합니다.
     *
     * @param memberSeq   현재 로그인한 회원 정보
     * @param diarySeq 수정할 일기의 고유 식별자
     * @param request  수정할 제목, 내용, 이미지, 태그 리스트 등을 담은 DTO
     * @throws BaseException 일기를 찾을 수 없거나 본인 일기가 아닐 경우 발생
     */
    @Transactional
    public void updateDiary(Long memberSeq, Long diarySeq, DiaryUpdateRequest request, MultipartFile image) {

        // 1. 본인의 일기인지 확인하며 조회
        Diary diary = diaryRepository.findByDiarySeqAndMember_MemberSeq(diarySeq, memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        String currentImageUrl = diary.getImageUrl();
        if(image != null && !image.isEmpty()) {
            // 기존 파일 삭제
            if(currentImageUrl != null) {
                imageService.deleteImage(currentImageUrl);
            }
            currentImageUrl = imageService.uploadImage(image);
        }

        // 2. 기본 정보 수정
        diary.updateDiary(request.title(), request.content(), request.diaryDate(), currentImageUrl);

        // 3. 태그 교체
        if (request.tags() != null) {
            List<Tag> tags = getOrCreateTags(request.tags());
            diary.updateTags(tags);
        }
    }

    /**
     * 태그 이름 리스트를 바탕으로 기존 태그를 조회하거나 신규 태그를 생성합니다.
     */
    private List<Tag> getOrCreateTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 이미 DB에 존재하는 태그들을 IN 쿼리로 한 번에 싹 다 가져옴 (쿼리 1방)
        List<Tag> existingTags = tagRepository.findByNameIn(tagNames);

        // 2. 찾아온 태그들의 이름만 추출
        List<String> existingTagNames = existingTags.stream()
                .map(Tag::getName)
                .toList();

        // 3. DB에 없는 새로운 태그들만 필터링해서 객체 생성
        List<Tag> newTags = tagNames.stream()
                .filter(name -> !existingTagNames.contains(name))
                .map(Tag::new)
                .toList();

        // 4. 새로운 태그들 한 번에 저장 (쿼리 1방 - Batch Insert 설정 시)
        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags);
            // 기존 태그 리스트에 새로 만든 태그들을 합침
            existingTags.addAll(newTags);
        }

        return existingTags;
    }

    /**
     * 특정 일기 삭제
     *  @param memberSeq   현재 로그인한 회원 정보정보
     *  @param diarySeq    삭제할 일기의 고유 식별자
     */
    @Transactional
    public void deleteDiary(Long memberSeq, Long diarySeq) {

        Diary diary = diaryRepository.findByDiarySeqAndMember_MemberSeq(diarySeq, memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        if(diary.getImageUrl() != null) {
            imageService.deleteImage(diary.getImageUrl());
        }
        diaryRepository.delete(diary);
        // diary_tag 테이블의 데이터도 알아서 같이 지워집니다!
    }

    /**
     * 일기의 상세 내용을 조회합니다.
     *
     * @param memberSeq   현재 로그인한 회원 정보
     * @param diarySeq 조회할 일기의 고유 식별자
     * @return 일기 상세 정보 및 연관된 태그 정보를 포함한 DTO
     * @throws BaseException 일기를 찾을 수 없거나 본인 일기가 아닐 경우 발생
     */
    @Transactional(readOnly = true)
    public DiaryDetailResponse getDiaryDetail(Long memberSeq, Long diarySeq) {
        Diary diary = diaryRepository.findDetailByDiarySeqAndMemberSeq(diarySeq, memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        return DiaryDetailResponse.from(diary);
    }

    /**
     *
     * @param memberSeq 현재 로그인한 회원 정보
     * @param year 조회할 년도
     * @param month 조회할 월
     * @return 조회 년월의 일기 개수 리스트
     */
    @Transactional(readOnly = true)
    public List<MonthlyDiaryCountResponse> getMonthlyDiaryStats(Long memberSeq, int year, int month) {

        // 1. 해당 월의 시작일 (예: 2024-03-01)
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);

        // 2. 해당 월의 마지막 날 (예: 2024-03-31)
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 3. 레포지토리 호출
        return diaryRepository.findAllMonthlyCount(memberSeq, startDate, endDate);
    }

    /**
     * 일기 목록에서 최근 사용한 태그를 보여줍니다.
     * @param memberSeq 현재 로그인한 회원 정보
     * @return 최근 30일 이내 가장 많이 사용한 태그 10개 (1. 빈도 수 2. 작성 순)
     */
    @Transactional(readOnly = true)
    public List<TagResponse> getRecentTopTags(Long memberSeq) {
        return tagRepository.findRecentTopTags(memberSeq);

    }
}
