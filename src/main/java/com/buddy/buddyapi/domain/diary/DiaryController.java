package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.global.common.ApiResponse;
import com.buddy.buddyapi.domain.diary.dto.DiaryCreateRequest;
import com.buddy.buddyapi.domain.diary.dto.DiaryGenerateRequest;
import com.buddy.buddyapi.domain.diary.dto.DiaryUpdateRequest;
import com.buddy.buddyapi.domain.diary.dto.DiaryDetailResponse;
import com.buddy.buddyapi.domain.diary.dto.DiaryListResponse;
import com.buddy.buddyapi.domain.diary.dto.DiaryPreviewResponse;
import com.buddy.buddyapi.domain.diary.dto.MonthlyDiaryCountResponse;
import com.buddy.buddyapi.global.security.CustomUserDetails;
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
            @AuthenticationPrincipal CustomUserDetails member,
            @Valid @RequestBody DiaryGenerateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(diaryService.generateDiaryFromChat(member.memberSeq(), request)));
    }

    @Operation(summary = "일기 생성", description = "새로운 일기를 저장합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createDiary(
            @AuthenticationPrincipal CustomUserDetails member,
            @RequestPart(value = "request") @Valid DiaryCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        Long diarySeq = diaryService.createDiary(member.memberSeq(), request, image);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("일기가 성공적으로 등록되었습니다.", diarySeq));
    }

    @Operation(summary = "날짜별 일기 목록 조회", description = "특정 날짜의 일기 리스트를 가져옵니다.")
    @GetMapping("/date")
    public ResponseEntity<ApiResponse<List<DiaryListResponse>>> getDiariesByDate(
            @AuthenticationPrincipal CustomUserDetails member,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(diaryService.getDiariesByDate(member.memberSeq(), date)));
    }

    @Operation(summary = "내 일기 목록 조회 및 검색", description = "무한 스크롤을 위한 페이징 처리된 일기 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Slice<DiaryListResponse>>> getDiaryList(
            @AuthenticationPrincipal CustomUserDetails member,
            @RequestParam(required = false) String search,
            // 기본값: 한 페이지에 10개씩, 작성일(diaryDate) 기준 최신순 정렬
            @ParameterObject @PageableDefault(size = 10, sort = "diaryDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Slice<DiaryListResponse> responses = diaryService.getDiaryList(member.memberSeq(), search, pageable);

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @Operation(summary = "월별 일기 개수 조회", description = "특정 월의 일기 개수를 가져옵니다.")
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<List<MonthlyDiaryCountResponse>>> getMonthlyDiaryStats(
            @AuthenticationPrincipal CustomUserDetails member, // 현재 로그인 유저
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month) {

        return ResponseEntity.ok(ApiResponse.ok(diaryService.getMonthlyDiaryStats(member.memberSeq(), year, month)));
    }

    @Operation(summary = "일기 상세 조회", description = "특정 일기의 상세 내용을 조회합니다.")
    @GetMapping("/{diarySeq}")
    public ResponseEntity<ApiResponse<DiaryDetailResponse>> getDiaryDetail(
            @AuthenticationPrincipal CustomUserDetails member,
            @PathVariable Long diarySeq) {
        return ResponseEntity.ok(ApiResponse.ok(diaryService.getDiaryDetail(member.memberSeq(), diarySeq)));
    }

    @Operation(summary = "일기 수정", description = "기존 일기의 내용 및 태그를 수정합니다.")
    @PatchMapping(value = "/{diarySeq}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> updateDiary(
            @AuthenticationPrincipal CustomUserDetails member,
            @PathVariable Long diarySeq,
            @RequestPart("request") @Valid DiaryUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        diaryService.updateDiary(member.memberSeq(), diarySeq, request, image);
        return ResponseEntity.ok(ApiResponse.ok("일기가 수정되었습니다.",diarySeq));
    }

    @Operation(summary = "일기 삭제", description = "일기를 삭제합니다.")
    @DeleteMapping("/{diarySeq}")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @AuthenticationPrincipal CustomUserDetails member,
            @PathVariable Long diarySeq) {
        diaryService.deleteDiary(member.memberSeq(), diarySeq);
        return ResponseEntity.ok(ApiResponse.ok("일기가 삭제되었습니다.", null));
    }





}
