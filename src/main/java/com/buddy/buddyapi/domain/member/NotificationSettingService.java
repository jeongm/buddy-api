package com.buddy.buddyapi.domain.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository settingRepository;

    public void createDefaultSetting(Member member, boolean isNightAgreed) {
        NotificationSetting defaultSetting = NotificationSetting.builder()
                .member(member)
                .chatAlertYn(true)
                .nightAlertYn(isNightAgreed)
                .dailyAlertYn(isNightAgreed)
                .marketingAlertYn(false)
                .build();
        settingRepository.save(defaultSetting);
    }

    /**
     * [온보딩 알림 설정 업데이트]
     * 유저가 온보딩 과정에서 선택한 야간/데일리 알림 동의 여부를 업데이트합니다.
     *
     * @param memberId 유저 식별자(PK)
     * @param isNightAgreed 야간 및 데일리 알림 수신 동의 여부
     */
    @Transactional
    public void updateSocialOnboardingSettings(Long memberId, boolean isNightAgreed) {
        // 알림 설정 가져오기 (메서드명은 현정님이 레포지토리에 만든 이름으로 맞춰주세요!)
        NotificationSetting setting = settingRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("알림 설정 정보를 찾을 수 없습니다."));

        // 온보딩에서 동의한 대로 야간 & 데일리 알림 상태 변경!
        setting.updateNightAlert(isNightAgreed);
        setting.updateDailyAlert(isNightAgreed);
    }
}
