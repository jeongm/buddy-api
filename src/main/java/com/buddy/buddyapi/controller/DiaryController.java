package com.buddy.buddyapi.controller;

import com.buddy.buddyapi.dto.common.ApiResponse;
import com.buddy.buddyapi.dto.request.DiaryCreateRequest;
import com.buddy.buddyapi.dto.request.DiaryUpdateRequest;
import com.buddy.buddyapi.dto.response.DiaryDetailResponse;
import com.buddy.buddyapi.dto.response.DiaryListResponse;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name="Diary", description = "일기 관련 API")
@RestController
@RequestMapping("/api/v1/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "일기 생성", description = "새로운 일기를 저장합니다.")
    @PostMapping
    public ApiResponse<Long> createDiary(
            @AuthenticationPrincipal Member member,
            @RequestBody DiaryCreateRequest request) {
        return ApiResponse.success(diaryService.createDiary(member, request));
    }

    @Operation(summary = "날짜별 일기 목록 조회", description = "특정 날짜의 일기 리스트를 가져옵니다.")
    @GetMapping
    public ApiResponse<List<DiaryListResponse>> getDiariesByDate(
            @AuthenticationPrincipal Member member,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ApiResponse.success(diaryService.getDiariesByDate(member, date));
    }

    @Operation(summary = "일기 상세 조회", description = "특정 일기의 상세 내용을 조회합니다.")
    @GetMapping("/{diarySeq}")
    public ApiResponse<DiaryDetailResponse> getDiaryDetail(
            @AuthenticationPrincipal Member member,
            @PathVariable Long diarySeq) {
        return ApiResponse.success(diaryService.getDiaryDetail(member, diarySeq));
    }

    @Operation(summary = "일기 수정", description = "기존 일기의 내용 및 태그를 수정합니다.")
    @PatchMapping("/{diarySeq}")
    public ApiResponse<Long> updateDiary(
            @AuthenticationPrincipal Member member,
            @PathVariable Long diarySeq,
            @RequestBody DiaryUpdateRequest request) {
        diaryService.updateDiary(member, diarySeq, request);
        return ApiResponse.success(diarySeq);
    }

    @Operation(summary = "일기 삭제", description = "일기를 삭제합니다.")
    @DeleteMapping("/{diarySeq}")
    public ApiResponse<Void> deleteDiary(
            @AuthenticationPrincipal Member member,
            @PathVariable Long diarySeq) {
        diaryService.deleteDiary(member, diarySeq);
        return ApiResponse.success(null);
    }



}
