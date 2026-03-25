package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.domain.chat.*;
import com.buddy.buddyapi.domain.diary.dto.*;
import com.buddy.buddyapi.domain.diary.event.DiaryImageUpdateEvent;
import com.buddy.buddyapi.domain.diary.event.DiaryImagesCleanupEvent;
import com.buddy.buddyapi.domain.member.Member;
import com.buddy.buddyapi.domain.member.MemberService;
import com.buddy.buddyapi.global.aspect.Timer;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.domain.ai.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final TagRepository tagRepository;

    private final MemberService memberService;
    private final AiService aiService;
    private final ChatService chatService;

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 채팅 내역을 기반으로 AI 일기 초안을 생성합니다. (DB 저장 안 함)
     *
     * @param memberId  현재 로그인한 회원 정보
     * @param request 일기 생성을 위한 세션 ID가 포함된 요청 DTO
     * @return AI가 생성한 일기 제목, 본문, 추천 태그 정보를 담은 프리뷰 응답 DTO
     * @throws BaseException 세션을 찾을 수 없거나 대화 내역이 비어있을 경우 발생
     */
    @Timer
    @Transactional(readOnly = true)
    public DiaryPreviewResponse generateDiaryFromChat(Long memberId, GenerateDiaryRequest request) {

        String fullConversation = chatService.formatChatHistory(request.sessionId(), memberId);

        // AI 서비스 호출 (페르소나와 대화 내용 전달)
        String rawResponse = aiService.getDiaryDraft(
                fullConversation
        );

        log.info("AI 응답 일기 초안 : \n{}",rawResponse);

        // AI 응답(JSON 문자열)을 DTO로 변환 (파싱 로직은 아래에서 구현)
        return parseAiResponse(rawResponse);
    }

    /**
     * 일기를 목록입니다. 검색어가 있을 시 검색된 일기 목록을 보여줍니다.
     * @param memberId 현재 로그인한 회원 정보
     * @param search 검색하고 싶은 내용
     * @param pageable 원하는 페이지
     * @return 다이어리 목록 (이미지, 100자가량의 내용, 제목, 태그)
     */
    @Transactional(readOnly = true)
    public Slice<DiaryListResponse> getDiaries(Long memberId, String search, Pageable pageable) {

        Slice<Diary> diarySlice = diaryRepository.searchMyDiaries(memberId, search, pageable);

        return diarySlice.map(DiaryListResponse::from);
    }

    /**
     * 특정 날짜에 작성된 일기 목록을 조회합니다.
     *
     * @param memberId 현재 로그인한 회원 정보
     * @param date   조회하고자 하는 날짜 (yyyy-MM-dd)
     * @return 해당 날짜에 작성된 일기 리스트 (최신순)
     */
    @Transactional(readOnly = true)
    public List<DiaryListResponse> getDiariesByDate(Long memberId, LocalDate date) {

        return diaryRepository.findAllByMemberAndDiaryDate(memberId, date)
                .stream()
                .map(DiaryListResponse::from)
                .toList();
    }

    /**
     * 사용자가 최종 확정한 일기 데이터를 DB에 저장합니다.
     *
     * @param memberId  현재 로그인한 회원 정보
     * @param request 저장할 일기 제목, 내용, 이미지, 태그 ID 리스트 등을 담은 DTO
     * @return 생성된 일기의 고유 식별자 (ID)
     * @throws BaseException 요청한 태그 ID가 존재하지 않을 경우 발생
     */
    @Transactional
    public Long createDiary(Long memberId, CreateDiaryRequest request, MultipartFile image) {

        Member member = memberService.getMemberById(memberId);

        ChatSession chatSession = null;
        if(request.sessionId() != null) {
            chatSession = chatService.getSession(request.sessionId(), memberId);
        }

        // 이미지 없이 DB 먼저 저장
        Diary diary = Diary.builder()
                .title(request.title())
                .content(request.content())
                .diaryDate(request.diaryDate())
                .imageUrl(null)
                .member(member)
                .chatSession(chatSession)
                .build();

        // 태그 리스트가 있다면 조회 후 연결
        if (request.tags() != null && !request.tags().isEmpty()) {
            List<Tag> tags = getOrCreateTags(request.tags());
            diary.addTags(tags);
        }

        Diary savedDiary = diaryRepository.save(diary);

        if (image != null && !image.isEmpty()) {
            eventPublisher.publishEvent(
                    new DiaryImageUpdateEvent(savedDiary.getDiaryId(), null, image)
            );
        }



        return savedDiary.getDiaryId();
    }

    /**
     * 기존에 작성된 일기 내용을 수정합니다.
     *
     * @param memberId   현재 로그인한 회원 정보
     * @param diaryId 수정할 일기의 고유 식별자
     * @param request  수정할 제목, 내용, 이미지, 태그 리스트 등을 담은 DTO
     * @throws BaseException 일기를 찾을 수 없거나 본인 일기가 아닐 경우 발생
     */
    @Transactional
    public void updateDiary(Long memberId, Long diaryId, UpdateDiaryRequest request, MultipartFile newImage) {

        Diary diary = diaryRepository.findByDiaryIdAndMember_MemberId(diaryId, memberId)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        diary.updateDiary(request.title(), request.content(), request.diaryDate(), diary.getImageUrl());


        // 태그 교체
        if (request.tags() != null) {
            List<Tag> tags = getOrCreateTags(request.tags());
            diary.updateTags(tags);
        }

        if (newImage != null && !newImage.isEmpty()) {
            eventPublisher.publishEvent(
                    new DiaryImageUpdateEvent(diaryId, diary.getImageUrl(), newImage)
            );
        } else if (Boolean.TRUE.equals(request.deleteImage())) {
            if (diary.getImageUrl() != null) {
                eventPublisher.publishEvent(
                        new DiaryImagesCleanupEvent(List.of(diary.getImageUrl()))
                );
                diary.updateDiary(request.title(), request.content(), request.diaryDate(), null);
            }
        }


    }

    /**
     * 특정 일기 삭제
     *  @param memberId   현재 로그인한 회원 정보정보
     *  @param diaryId    삭제할 일기의 고유 식별자
     */
    @Transactional
    public void deleteDiary(Long memberId, Long diaryId) {

        Diary diary = diaryRepository.findByDiaryIdAndMember_MemberId(diaryId, memberId)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        String imageUrl = diary.getImageUrl();

        diaryRepository.delete(diary);

        if(diary.getImageUrl() != null) {
            eventPublisher.publishEvent(new DiaryImagesCleanupEvent(List.of(imageUrl)));
        }
    }

    /**
     * 일기의 상세 내용을 조회합니다.
     *
     * @param memberId   현재 로그인한 회원 정보
     * @param diaryId 조회할 일기의 고유 식별자
     * @return 일기 상세 정보 및 연관된 태그 정보를 포함한 DTO
     * @throws BaseException 일기를 찾을 수 없거나 본인 일기가 아닐 경우 발생
     */
    @Transactional(readOnly = true)
    public DiaryDetailResponse getDiaryDetail(Long memberId, Long diaryId) {
        Diary diary = diaryRepository.findDetailByDiaryIdAndMemberId(diaryId, memberId)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        return DiaryDetailResponse.from(diary);
    }

    /**
     *
     * @param memberId 현재 로그인한 회원 정보
     * @param year 조회할 년도
     * @param month 조회할 월
     * @return 조회 년월의 일기 개수 리스트
     */
    @Transactional(readOnly = true)
    public List<MonthlyDiaryCountResponse> getMonthlyDiaryStats(Long memberId, int year, int month) {

        // 해당 월의 시작일 (예: 2024-03-01)
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);

        // 해당 월의 마지막 날 (예: 2024-03-31)
        LocalDate endDate = yearMonth.atEndOfMonth();

        return diaryRepository.findAllMonthlyCount(memberId, startDate, endDate);
    }

    /**
     * 일기 목록에서 최근 사용한 태그를 보여줍니다.
     * @param memberId 현재 로그인한 회원 정보
     * @return 최근 30일 이내 가장 많이 사용한 태그 10개 (1. 빈도 수 2. 작성 순)
     */
    @Transactional(readOnly = true)
    public List<TagResponse> getRecentTopTags(Long memberId) {
        return diaryRepository.findRecentTopTags(memberId);

    }


    /**
     * 태그 이름 리스트를 바탕으로 기존 태그를 조회하거나 신규 태그를 생성합니다.
     */
    private List<Tag> getOrCreateTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptyList(); // 텅 빈 리스트는 메모리를 안 먹는 emptyList() 반환
        }

        // 이미 DB에 존재하는 태그들을 IN 쿼리로 한 번에 싹 다 가져옴
        List<Tag> existingTags = tagRepository.findByNameIn(tagNames);

        // 찾아온 태그들의 이름만 추출
        List<String> existingTagNames = existingTags.stream()
                .map(Tag::getName)
                .toList();

        // DB에 없는 새로운 태그들만 필터링해서 객체 생성
        List<Tag> newTags = tagNames.stream()
                .filter(name -> !existingTagNames.contains(name))
                .map(Tag::new)
                .toList();

        // 새로운 태그들 한 번에 저장 (쿼리 1방 - Batch Insert 설정 시)
        if(newTags.isEmpty()) {
            return existingTags;
        }

        tagRepository.saveAll(newTags);

        return Stream.concat(existingTags.stream(), newTags.stream())
                .toList();
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
            int startIndex = jsonString.indexOf("{");
            int endIndex = jsonString.lastIndexOf("}");
            if (startIndex == -1 || endIndex == -1) throw new BaseException(ResultCode.AI_PARSE_ERROR);

            String cleanedJson = jsonString.substring(startIndex, endIndex + 1);
            ParsedAiDiaryDto parsed = objectMapper.readValue(cleanedJson, ParsedAiDiaryDto.class);

            // 태그 또한 우선 프리 뷰 단계이므로 저장하지 않고 이름만 반환합니다
            List<TagResponse> tagResponses = parsed.tags().stream()
                    .map(name -> new TagResponse(null, name))
                    .toList();

            return new DiaryPreviewResponse(parsed.title(), parsed.content(), tagResponses);
        } catch (Exception e) {
            // 파싱 실패 시 기본 응답을 주거나 예외 처리
            log.error("AI 응답 파싱 에러. 원본 데이터: {}", jsonString, e);
            throw new BaseException(ResultCode.AI_PARSE_ERROR);
        }
    }

    private record ParsedAiDiaryDto(String title, String content, List<String> tags) {}
}
