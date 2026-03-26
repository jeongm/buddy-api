package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.domain.diary.dto.*;
import com.buddy.buddyapi.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name="Diary", description = "일기 관련 API")
@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "대화 기반 AI 일기 생성", description = "대화 세션을 기반으로 AI가 일기 초안과 태그를 생성합니다.")
    @PostMapping("/from-chat")
    public ResponseEntity<ApiResponse<DiaryPreviewResponse>> generateDiaryFromChat(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody GenerateDiaryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(diaryService.generateDiaryFromChat(memberId, request)));
    }

    @Operation(summary = "일기 생성", description = "새로운 일기를 저장합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createDiary(
            @AuthenticationPrincipal Long memberId,
            @RequestPart(value = "request") @Valid CreateDiaryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        Long diaryId = diaryService.createDiary(memberId, request, image);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("일기가 성공적으로 등록되었습니다.", diaryId));
    }

    @Operation(summary = "날짜별 일기 목록 조회", description = "특정 날짜의 일기 리스트를 가져옵니다.")
    @GetMapping("/date")
    public ResponseEntity<ApiResponse<List<DiaryListResponse>>> getDiariesByDate(
            @AuthenticationPrincipal Long memberId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(diaryService.getDiariesByDate(memberId, date)));
    }

    @Operation(summary = "내 일기 목록 조회 및 검색", description = "무한 스크롤을 위한 페이징 처리된 일기 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Slice<DiaryListResponse>>> getDiaryList(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(name = "search", required = false) String search,
            // 기본값: 한 페이지에 10개씩, 작성일(diaryDate) 기준 최신순 정렬
            @ParameterObject @PageableDefault(size = 10, sort = "diaryDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Slice<DiaryListResponse> responses = diaryService.getDiaries(memberId, search, pageable);

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @Operation(summary = "월별 일기 개수 조회", description = "특정 월의 일기 개수를 가져옵니다.")
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<List<MonthlyDiaryCountResponse>>> getMonthlyDiaryStats(
            @AuthenticationPrincipal Long memberId, // 현재 로그인 유저
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month) {

        return ResponseEntity.ok(ApiResponse.ok(diaryService.getMonthlyDiaryStats(memberId, year, month)));
    }

    @Operation(summary = "일기 상세 조회", description = "특정 일기의 상세 내용을 조회합니다.")
    @GetMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<DiaryDetailResponse>> getDiaryDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long diaryId) {
        return ResponseEntity.ok(ApiResponse.ok(diaryService.getDiaryDetail(memberId, diaryId)));
    }

    @Operation(summary = "일기 수정", description = "기존 일기의 내용 및 태그를 수정합니다.")
    @PatchMapping(value = "/{diaryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> updateDiary(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long diaryId,
            @RequestPart("request") @Valid UpdateDiaryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        diaryService.updateDiary(memberId, diaryId, request, image);
        return ResponseEntity.ok(ApiResponse.ok("일기가 수정되었습니다.",diaryId));
    }

    @Operation(summary = "일기 삭제", description = "일기를 삭제합니다.")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long diaryId) {
        diaryService.deleteDiary(memberId, diaryId);
        return ResponseEntity.ok(ApiResponse.ok("일기가 삭제되었습니다.", null));
    }

    @Operation(summary = "다이어리 추천 태그 조회", description = "최근 30일 동안 사용자가 가장 많이 사용한 태그 TOP 10을 조회합니다.")
    @GetMapping("/tags/recommend")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getRecentTopTags(
            @AuthenticationPrincipal Long memberId
    ) {
        List<TagResponse> result = diaryService.getRecentTopTags(memberId);

        return ResponseEntity.ok(ApiResponse.ok("태그 조회 성공",result));
    }
}
