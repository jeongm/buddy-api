package com.buddy.buddyapi.domain.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    // 1. 대화 소멸 경고 알림 (10시간 경과 시)
    @Column(name = "chat_alert_yn", nullable = false)
    private boolean chatAlertYn = true;

    // 2. 데일리 안부 알림
    @Column(name = "daily_alert_yn", nullable = false)
    private boolean dailyAlertYn = false;

    // 마케팅/이벤트 알림
    @Column(name = "marketing_alert_yn", nullable = false)
    private boolean marketingAlertYn = false;

    // 야간 알림 수신 동의 (밤 9시 ~ 아침 8시 사이 수신 여부)
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
