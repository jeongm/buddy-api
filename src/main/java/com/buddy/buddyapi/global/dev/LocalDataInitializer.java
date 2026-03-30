package com.buddy.buddyapi.global.dev;

import com.buddy.buddyapi.domain.character.BuddyCharacter;
import com.buddy.buddyapi.domain.character.BuddyCharacterRepository;
import com.buddy.buddyapi.domain.diary.Diary;
import com.buddy.buddyapi.domain.diary.DiaryRepository;
import com.buddy.buddyapi.domain.insight.MemberInsight;
import com.buddy.buddyapi.domain.insight.MemberInsightRepository;
import com.buddy.buddyapi.domain.member.Member;
import com.buddy.buddyapi.domain.member.MemberRepository;
import com.buddy.buddyapi.domain.member.NotificationSetting;
import com.buddy.buddyapi.domain.member.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * dev 프로파일 전용 초기 데이터 세팅.
 * Swagger UI로 바로 테스트할 수 있도록 온보딩 완료 상태의 계정을 생성한다.
 */

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final BuddyCharacterRepository characterRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final MemberInsightRepository memberInsightRepository;
    private final DiaryRepository diaryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("===== [LocalDataInitializer] 테스트 데이터 삽입 시작 =====");

        List<BuddyCharacter> characters = characterRepository.findAll();

        Member member1 = initMember("je0ng22@naver.com", "1234", "테스트계정", characters.get(0));
        Member member2 = initMember("buddyzzang11@gmail.com", "1234", "버디짱", characters.get(1));

        initNotificationSetting(member1, true, false, false, true);
        initNotificationSetting(member2, true, true, true, true);

        initMemberInsight(member1);
        initMemberInsight(member2);

        initDiaries(member1);

        log.info("===== [LocalDataInitializer] 테스트 데이터 삽입 완료 =====");
        log.info("계정1 - email: je0ng22@naver.com / password: 1234");
        log.info("계정2 - email: buddyzzang11@gmail.com / password: 1234");
    }

    /**
     * 온보딩 완료 상태의 회원을 생성한다. (캐릭터 선택 완료)
     *
     * @param email     이메일
     * @param password  평문 비밀번호
     * @param nickname  닉네임
     * @return 저장된 회원 엔티티
     */
    private Member initMember(String email, String password, String nickname, BuddyCharacter character) {
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .build();

        member.updateCharacter(character);
        member.updateCharacterNickname("BUDDY");
        member.updatePushToken("test-push-token-1234");

        return memberRepository.save(member);
    }

    /**
     * 회원 알림 설정을 생성한다.
     *
     * @param member           대상 회원
     * @param chatAlertYn      대화 소멸 경고 알림 여부
     * @param dailyAlertYn     데일리 안부 알림 여부
     * @param marketingAlertYn 마케팅 알림 여부
     * @param nightAlertYn     야간 알림 여부
     */
    private void initNotificationSetting(Member member, boolean chatAlertYn, boolean dailyAlertYn,
                                         boolean marketingAlertYn, boolean nightAlertYn) {
        notificationSettingRepository.save(NotificationSetting.builder()
                .member(member)
                .chatAlertYn(chatAlertYn)
                .dailyAlertYn(dailyAlertYn)
                .marketingAlertYn(marketingAlertYn)
                .nightAlertYn(nightAlertYn)
                .build());
    }

    /**
     * 회원 인사이트 초기 레코드를 생성한다.
     * weekly_identity, weekly_keyword 는 null (아직 생성 전 상태)
     *
     * @param member 대상 회원
     */
    private void initMemberInsight(Member member) {
        memberInsightRepository.save(MemberInsight.builder()
                .member(member)
                .weeklyIdentity(null)
                .weeklyKeyword(null)
                .updatedAt(null)
                .build());
    }

    /**
     * streak 테스트용 일기 데이터를 생성한다.
     * 현재 연속 기록 5일 + 과거 최고 기록 10일 구간 포함.
     * member2 는 일기 없는 상태로 streak 0 케이스 테스트 가능.
     *
     * @param member 대상 회원
     */
    private void initDiaries(Member member) {
        LocalDate today = LocalDate.now();

        // 현재 연속 기록: 최근 5일
        for (int i = 0; i < 5; i++) {
            diaryRepository.save(Diary.builder()
                    .member(member)
                    .title(today.minusDays(i) + " 일기")
                    .content("테스트 내용")
                    .diaryDate(today.minusDays(i))
                    .build());
        }

        // 과거 최고 기록: 10일 연속 (2달 전)
        LocalDate pastStart = today.minusDays(60);
        for (int i = 0; i < 10; i++) {
            diaryRepository.save(Diary.builder()
                    .member(member)
                    .title(pastStart.plusDays(i) + " 일기")
                    .content("테스트 내용")
                    .diaryDate(pastStart.plusDays(i))
                    .build());
        }
        log.info("테스트 일기 생성 완료 - currentStreak: 5일, bestStreak: 10일 확인 가능");
    }
}