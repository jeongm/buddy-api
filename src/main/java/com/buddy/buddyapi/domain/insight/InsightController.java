package com.buddy.buddyapi.domain.insight;

import com.buddy.buddyapi.domain.insight.dto.TagNameCountResponse;
import com.buddy.buddyapi.domain.insight.dto.WeeklyIdentityResponse;
import com.buddy.buddyapi.global.common.ApiResponse;
import com.buddy.buddyapi.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name="Insight", description = "통계 관련 API")
@RestController
@RequestMapping("/api/v1/insight")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    @Operation(summary = "최다 빈도 태그 조회", description = "최근 7일 동안 사용자가 가장 많이 사용한 태그 TOP 5을 조회합니다.")
    @GetMapping("/weekly/tags")
    public ResponseEntity<ApiResponse<List<TagNameCountResponse>>> getLastWeekTopTags(
            @AuthenticationPrincipal CustomUserDetails member
    ) {
        List<TagNameCountResponse> result = insightService.getLastWeekTopTags(member.memberId());

        return ResponseEntity.ok(ApiResponse.ok("태그 조회 성공",result));
    }

    @Operation(
            summary = "주간 아이덴티티(칭호) 조회",
            description = "유저의 지난주 일기를 바탕으로 AI가 분석한 주간 칭호와 핵심 태그를 반환합니다. (이번 주 최초 조회 시에만 AI 분석이 실행되며, 이후에는 저장된 데이터를 빠르게 반환합니다. 작성된 일기가 없으면 null이 반환됩니다.)"
    )
    @GetMapping("/weekly/identity")
    public ResponseEntity<ApiResponse<WeeklyIdentityResponse>> getMyWeeklyIdentity(
            @AuthenticationPrincipal CustomUserDetails member) {

        WeeklyIdentityResponse response = insightService.getOrUpdateWeeklyInsight(member.memberId());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
