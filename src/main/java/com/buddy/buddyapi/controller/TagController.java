package com.buddy.buddyapi.controller;

import com.buddy.buddyapi.dto.common.ApiResponse;
import com.buddy.buddyapi.dto.response.TagResponse;
import com.buddy.buddyapi.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tag", description = "태그 관련 API")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "태그 목록 조회", description = "서비스에서 제공하는 모든 태그 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags() {
        return ResponseEntity.ok(ApiResponse.ok(tagService.getAllTags()));
    }
}
