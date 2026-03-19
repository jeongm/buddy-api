package com.buddy.buddyapi.domain.member.dto;

import com.buddy.buddyapi.domain.member.NotificationSetting;

/**
 * 알림 설정 전체 상태 조회 응답 DTO.
 *
 * @param chatAlert      대화 소멸 경고 알림 활성화 여부
 * @param dailyAlert     데일리 안부 알림 활성화 여부
 * @param marketingAlert 마케팅/이벤트 알림 활성화 여부
 * @param nightAlert     야간 알림 수신 동의 여부
 */
public record NotificationSettingResponse(
        boolean chatAlert,
        boolean dailyAlert,
        boolean marketingAlert,
        boolean nightAlert
) {
    /**
     * NotificationSetting 엔티티로부터 응답 DTO를 생성합니다.
     *
     * @param setting 알림 설정 엔티티
     * @return NotificationSettingResponse
     */
    public static NotificationSettingResponse from(NotificationSetting setting) {
        return new NotificationSettingResponse(
                setting.isChatAlertYn(),
                setting.isDailyAlertYn(),
                setting.isMarketingAlertYn(),
                setting.isNightAlertYn()
        );
    }
}
