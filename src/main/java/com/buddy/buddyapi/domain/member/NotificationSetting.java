package com.buddy.buddyapi.domain.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_setting")
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    // 1. 대화 소멸 경고 알림 (10시간 경과 시)
// -> 서비스 필수 알림이므로 기본값 true
    @Column(name = "chat_alert_yn", nullable = false)
    private boolean chatAlertYn = true;

    // 2. 데일리 안부 알림 (우리가 기획한 "오늘 하루 어땠어?")
// -> 선택적 기능이므로 프론트에서 동의받아야 함 (기본값 false 또는 가입 시 받음)
    @Column(name = "daily_alert_yn", nullable = false)
    private boolean dailyAlertYn = false;

    // 3. 마케팅/이벤트 알림 (나중에 무조건 추가해 달라고 기획자가 조름)
// -> 미리 만들어두면 "확장성을 고려한 설계"로 포폴 어필 가능 (기본값 false)
    @Column(name = "marketing_alert_yn", nullable = false)
    private boolean marketingAlertYn = false;

    // 4. 야간 알림 수신 동의 (밤 9시 ~ 아침 8시 사이 수신 여부)
// -> 데일리 푸시(밤 9시 30분)를 받으려면 이게 true여야 함! (기본값 false)
    @Column(name = "night_alert_yn", nullable = false)
    private boolean nightAlertYn = false;

    @Builder
    public NotificationSetting(Member member, boolean chatAlertYn, boolean dailyAlertYn, boolean marketingAlertYn, boolean nightAlertYn) {
        this.member = member;
        this.chatAlertYn = chatAlertYn;
        this.dailyAlertYn = dailyAlertYn;
        this.marketingAlertYn = marketingAlertYn;
        this.nightAlertYn = nightAlertYn;
    }


    public void updateChatAlert(boolean status) {
        this.chatAlertYn = status;
    }

    public void updateDailyAlert(boolean status) {
        this.dailyAlertYn = status;
    }

    public void updateMarketingAlert(boolean status) {
        this.marketingAlertYn = status;
    }

    public void updateNightAlert(boolean status) {
        this.nightAlertYn = status;
    }


}
