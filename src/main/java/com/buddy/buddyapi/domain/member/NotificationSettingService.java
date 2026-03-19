package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.domain.member.dto.NotificationSettingResponse;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository settingRepository;

    /**
     * 회원가입/소셜 로그인 시 기본 알림 설정을 생성합니다.
     * chatAlertYn은 항상 true, dailyAlertYn은 야간 동의 여부와 동일하게 설정됩니다.
     *
     * @param member        알림 설정을 생성할 회원 엔티티
     * @param isNightAgreed 야간 알림 수신 동의 여부
     */
    @Transactional
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
    public void updateOnboardingSettings(Long memberId, boolean isNightAgreed) {
        // 알림 설정 가져오기 (메서드명은 현정님이 레포지토리에 만든 이름으로 맞춰주세요!)
        NotificationSetting setting = findSettingOrThrow(memberId);

        // 온보딩에서 동의한 대로 야간 & 데일리 알림 상태 변경!
        setting.updateNightAlert(isNightAgreed);
        setting.updateDailyAlert(isNightAgreed);
    }

    /**
     * 현재 로그인한 회원의 알림 설정 전체를 조회합니다.
     *
     * @param memberId 로그인한 회원 ID
     * @return 알림 설정 전체 상태 응답 DTO
     */
    @Transactional(readOnly = true)
    public NotificationSettingResponse getNotificationSetting(Long memberId) {
        return NotificationSettingResponse.from(findSettingOrThrow(memberId));
    }

    /**
     * 대화 소멸 경고 알림 상태를 변경합니다.
     *
     * @param memberId 로그인한 회원 ID
     * @param enabled  변경할 활성화 상태
     * @return 변경 후 알림 설정 전체 상태
     */
    @Transactional
    public NotificationSettingResponse updateChatAlert(Long memberId, boolean enabled) {
        NotificationSetting setting = findSettingOrThrow(memberId);
        setting.updateChatAlert(enabled);
        return NotificationSettingResponse.from(setting);
    }

    /**
     * 데일리 안부 알림 상태를 변경합니다.
     * 야간 알림이 꺼진 상태에서 데일리 알림을 켜는 것은 불가합니다.
     * (데일리 안부 알림은 밤 9시 30분에 발송되므로 야간 알림 동의가 선행되어야 합니다.)
     *
     * @param memberId 로그인한 회원 ID
     * @param enabled  변경할 활성화 상태
     * @return 변경 후 알림 설정 전체 상태
     * @throws BaseException NIGHT_ALERT_REQUIRED - 야간 알림 미동의 상태에서 데일리 알림을 켜려 할 경우
     */
    @Transactional
    public NotificationSettingResponse updateDailyAlert(Long memberId, boolean enabled) {
        NotificationSetting setting = findSettingOrThrow(memberId);

        if (enabled && !setting.isNightAlertYn()) {
            throw new BaseException(ResultCode.NIGHT_ALERT_REQUIRED);
        }

        setting.updateDailyAlert(enabled);
        return NotificationSettingResponse.from(setting);
    }

    /**
     * 마케팅/이벤트 알림 상태를 변경합니다.
     *
     * @param memberId 로그인한 회원 ID
     * @param enabled  변경할 활성화 상태
     * @return 변경 후 알림 설정 전체 상태
     */
    @Transactional
    public NotificationSettingResponse updateMarketingAlert(Long memberId, boolean enabled) {
        NotificationSetting setting = findSettingOrThrow(memberId);
        setting.updateMarketingAlert(enabled);
        return NotificationSettingResponse.from(setting);
    }

    /**
     * 야간 알림 수신 동의 상태를 변경합니다.
     * 야간 알림을 끄면 데일리 안부 알림도 함께 비활성화됩니다.
     *
     * @param memberId 로그인한 회원 ID
     * @param enabled  변경할 활성화 상태
     * @return 변경 후 알림 설정 전체 상태
     */
    @Transactional
    public NotificationSettingResponse updateNightAlert(Long memberId, boolean enabled) {
        NotificationSetting setting = findSettingOrThrow(memberId);
        setting.updateNightAlert(enabled);

        if (!enabled) {
            setting.updateDailyAlert(false);
        }

        return NotificationSettingResponse.from(setting);
    }


    /**
     * [회원 탈퇴] 회원의 알림 설정을 DB에서 물리적으로 삭제합니다.
     *
     * @param memberId 탈퇴할 회원 ID
     */
    @Transactional
    public void deleteSettingOnWithdrawal(Long memberId) {
        settingRepository.deleteByMember_MemberId(memberId);
    }

    /**
     * 회원 ID로 알림 설정을 조회하고, 없으면 예외를 던지는 내부 공통 메서드.
     *
     * @param memberId 회원 ID
     * @return NotificationSetting 엔티티
     * @throws BaseException NOTIFICATION_SETTING_NOT_FOUND
     */
    private NotificationSetting findSettingOrThrow(Long memberId) {
        return settingRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new BaseException(ResultCode.NOTIFICATION_SETTING_NOT_FOUND));
    }
}
