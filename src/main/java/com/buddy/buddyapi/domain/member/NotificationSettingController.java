package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.domain.member.dto.NotificationSettingResponse;
import com.buddy.buddyapi.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "알림 설정 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationSettingController {
    private final NotificationSettingService notificationSettingService;

    @Operation(summary = "알림 설정 전체 조회", description = "현재 로그인한 사용자의 알림 설정 상태를 전체 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> getNotificationSetting(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {

        return ResponseEntity.ok(ApiResponse.ok("알림 설정 조회 성공",
                notificationSettingService.getNotificationSetting(memberId)));
    }

    @Operation(summary = "대화 소멸 경고 알림 변경", description = "대화가 10시간 경과 시 발송되는 소멸 경고 알림을 켜거나 끕니다.")
    @PatchMapping("/chat")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateChatAlert(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam boolean enabled) {

        return ResponseEntity.ok(ApiResponse.ok("대화 소멸 경고 알림 설정이 변경되었습니다.",
                notificationSettingService.updateChatAlert(memberId, enabled)));
    }

    @Operation(summary = "데일리 안부 알림 변경", description = "매일 발송되는 데일리 안부 알림을 켜거나 끕니다.")
    @PatchMapping("/daily")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateDailyAlert(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam boolean enabled) {

        return ResponseEntity.ok(ApiResponse.ok("데일리 안부 알림 설정이 변경되었습니다.",
                notificationSettingService.updateDailyAlert(memberId, enabled)));
    }

    @Operation(summary = "마케팅/이벤트 알림 변경", description = "마케팅 및 이벤트 알림을 켜거나 끕니다.")
    @PatchMapping("/marketing")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateMarketingAlert(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam boolean enabled) {

        return ResponseEntity.ok(ApiResponse.ok("마케팅/이벤트 알림 설정이 변경되었습니다.",
                notificationSettingService.updateMarketingAlert(memberId, enabled)));
    }

    @Operation(summary = "야간 알림 수신 동의 변경", description = "밤 9시 ~ 아침 8시 사이의 야간 알림 수신 여부를 변경합니다.")
    @PatchMapping("/night")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateNightAlert(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(ApiResponse.ok("야간 알림 수신 설정이 변경되었습니다.",
                notificationSettingService.updateNightAlert(memberId, enabled)));
    }
}
