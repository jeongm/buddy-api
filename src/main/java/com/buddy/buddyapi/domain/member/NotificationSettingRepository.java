package com.buddy.buddyapi.domain.member;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByMember_MemberId(Long memberId);

    @Query("SELECT ns FROM NotificationSetting ns JOIN FETCH ns.member m " +
            "WHERE ns.nightAlertYn = true " +
            "AND ns.dailyAlertYn = true " +
            "AND m.pushToken IS NOT NULL")
    List<NotificationSetting> findTargetSettingsForDailyPush();

    void deleteByMember_MemberId(Long memberId);

}
