package com.buddy.buddyapi.global.scheduler;

import com.buddy.buddyapi.domain.member.NotificationSetting;
import com.buddy.buddyapi.domain.member.NotificationSettingRepository;
import com.buddy.buddyapi.global.infra.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Component
public class DailyPushScheduler {

    private final NotificationSettingRepository settingRepository;

    private final FcmService fcmService;

    private static final Random RANDOM = new Random();

    private final List<String> pushMessages = Arrays.asList(
            "오늘 하루도 정말 고생 많았어요! 버디가 이야기 들을 준비하고 있어요 🌙",
            "문득 생각나서 연락했어요. 오늘 하루, 특별한 일은 없었나요? 💭",
            "많이 지친 하루였나요? 버디에게 훌훌 털어놓고 편하게 자요 🥺",
            "짠! 버디가 왔어요! 오늘 있었던 일 하나만 알려줄래요? 🥰"
    );

    /**
     * 매일 밤 9시 30분에 전체 유저에게 랜덤 안부 인사 발송!
     */
    @Scheduled(cron = "0 30 21 * * *", zone = "Asia/Seoul") // 한국 시간 밤 9시 30분!
    public void sendDailyNightGreeting() {
        // 알림 수신 동의한 유저 대상
        List<NotificationSetting> targetSettings = settingRepository.findTargetSettingsForDailyPush();

        if (targetSettings.isEmpty()) return;

        List<String> targetTokens = targetSettings.stream()
                .map(setting -> setting.getMember().getPushToken())
                .filter(token -> token != null && !token.isBlank()) // ✅
                .toList();

        log.info("🌙 발송 대상자 수: {}명", targetTokens.size());

        String randomMessage = pushMessages.get(RANDOM.nextInt(pushMessages.size()));

        log.info("🌙 데일리 안부 푸시 발송! 대상: {}명, 멘트: {}", targetTokens.size(), randomMessage);
        fcmService.sendPushBulk(targetTokens, "버디 🐶", randomMessage);
    }

}
