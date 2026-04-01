package com.buddy.buddyapi.domain.insight;

import com.buddy.buddyapi.domain.ai.AiService;
import com.buddy.buddyapi.domain.diary.DiaryQueryService;
import com.buddy.buddyapi.domain.diary.DiaryRepository;
import com.buddy.buddyapi.domain.insight.dto.StreakResponse;
import com.buddy.buddyapi.domain.insight.dto.TagNameCountResponse;
import com.buddy.buddyapi.domain.insight.dto.WeeklyIdentityResponse;
import com.buddy.buddyapi.domain.member.Member;
import com.buddy.buddyapi.domain.member.MemberService;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberInsightService {

    private final MemberInsightRepository memberInsightRepository;

    private final MemberService memberService;
    private final DiaryQueryService diaryQueryService;
    private final AiService aiService;

    private final ObjectMapper objectMapper;


    /**
     * 주간 아이덴티티(칭호) 조회 및 생성 (Lazy Evaluation)
     */
    @Transactional
    public WeeklyIdentityResponse getOrUpdateWeeklyInsight(Long memberId) {
        Member member = memberService.getMemberById(memberId);

        // 유저의 통계 테이블 조회 (없으면 null)
        MemberInsight insight = memberInsightRepository.findByMember_MemberId(memberId)
                .orElse(null);


        // 이미 이번 주에 칭호를 발급받았다면? -> AI 호출 없이 DB 값 바로 리턴!
        if (isCacheValid(insight)) {
            return WeeklyIdentityResponse.from(insight);
        }

        // 이번 주에 처음 접속했거나, 아직 데이터가 없는 경우

        // 객체 준비: 없으면 미리 한 번만 깔끔하게 만들어두기
        if (insight == null) {
            insight = MemberInsight.builder().member(member).build();
        }

        // 저번 주 월~일 계산
        LocalDate startOfLastWeek = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate endOfLastWeek = LocalDate.now().minusWeeks(1).with(DayOfWeek.SUNDAY);

        // 저번 주 일기 긁어오기
        List<String> diaryContents = diaryQueryService.getDiaryContentsByDateRange(memberId, startOfLastWeek, endOfLastWeek);
        // 저번 주에 쓴 일기가 없을 시
        if (diaryContents.isEmpty()) {
            return updateAndSaveInsight(insight,null,null); // 프론트엔드엔 null이 담긴 예쁜 DTO가 감
        }

        // 대망의 AI 호출
        String rawJsonResponse = aiService.getWeeklyIdentityDraft(diaryContents);

        // JSON 파싱 및 DB 업데이트
        ParsedInsightDto parsedDto = parseAiResponse(rawJsonResponse);

        return updateAndSaveInsight(insight, parsedDto.weeklyIdentity(), parsedDto.weeklyKeyword());
    }

    /**
     * 지난주(월~일) 동안 가장 많이 사용한 태그 Top 5와 빈도수를 조회합니다.
     * @param memberId 현재 로그인한 회원 정보
     * @return 태그 이름과 사용 횟수가 담긴 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<TagNameCountResponse> getLastWeekTopTags(Long memberId) {

        // 지난주 월~일 날짜 계산
        LocalDate lastMonday = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastSunday = LocalDate.now().minusWeeks(1).with(DayOfWeek.SUNDAY);

        return diaryQueryService.getTopTagsByDateRange(memberId, lastMonday, lastSunday, 5);
    }

    /**
     * streak 조회 - DB를 수정하지 않고 현재 상태를 계산해서 반환한다.
     * lastDiaryDate 기준으로 오늘/어제가 아니면 currentStreak를 0으로 내려준다.
     *
     * @param memberId 조회할 회원 PK
     * @return currentStreak, bestStreak DTO
     */
    @Transactional(readOnly = true)
    public StreakResponse getStreak(Long memberId) {
        MemberInsight insight = memberInsightRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new BaseException(ResultCode.MEMBER_INSIGHT_NOT_FOUND));

        LocalDate today = LocalDate.now();
        int currentStreak = isStreakActive(insight.getLastDiaryDate(), today)
                ? insight.getCurrentStreak()
                : 0;

        return new StreakResponse(currentStreak, insight.getBestStreak());
    }

    /**
     * 일기 저장 후 streak 갱신.
     * 오늘/어제 날짜면 O(1) 단순 계산, 과거 날짜면 전체 재계산.
     *
     * @param memberId  회원 PK
     * @param diaryDate 저장된 일기 날짜
     */
    @Transactional
    public void updateStreakOnCreate(Long memberId, LocalDate diaryDate) {
        MemberInsight insight = memberInsightRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new BaseException(ResultCode.MEMBER_INSIGHT_NOT_FOUND));

        LocalDate lastDiaryDate = insight.getLastDiaryDate();

        // 오늘 이미 쓴 경우 무시
        if (diaryDate.equals(lastDiaryDate)) {
            return;
        }

        // 오늘/어제 날짜면 단순 계산
        if (lastDiaryDate == null || diaryDate.equals(LocalDate.now()) || diaryDate.equals(LocalDate.now().minusDays(1))) {
            long daysBetween = lastDiaryDate == null ? Long.MAX_VALUE
                    : java.time.temporal.ChronoUnit.DAYS.between(lastDiaryDate, diaryDate);

            int newStreak = (daysBetween == 1) ? insight.getCurrentStreak() + 1 : 1;
            insight.updateStreak(newStreak, maxDate(lastDiaryDate, diaryDate));
        } else {
            // 과거 날짜면 전체 재계산
            syncStreak(memberId, insight);
        }
    }

    /**
     * 일기 삭제 후 streak 재계산.
     * currentStreak만 재계산하고 bestStreak는 절대 깎지 않는다.
     *
     * @param memberId 회원 PK
     */
    @Transactional
    public void syncStreakOnDelete(Long memberId) {
        MemberInsight insight = memberInsightRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new BaseException(ResultCode.MEMBER_INSIGHT_NOT_FOUND));

        syncStreak(memberId, insight);
    }



    /**
     * 캐시 유효성 검사
     */
    private boolean isCacheValid(MemberInsight insight) {
        // 이번 주 월요일 00:00:00 (시간 비교를 위해 LocalDateTime)
        LocalDateTime startOfThisWeek = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        return insight != null && insight.getWeeklyUpdatedAt() != null && insight.getWeeklyUpdatedAt().isAfter(startOfThisWeek);
    }

    /**
     * 엔티티 업데이트 및 저장 (그리고 DTO 변환까지 한 큐에!)
     */
    private WeeklyIdentityResponse updateAndSaveInsight(MemberInsight insight, String identity, String keyword) {
        insight.updateWeeklyInsight(identity, keyword);
        memberInsightRepository.save(insight);
        return WeeklyIdentityResponse.from(insight);
    }

    private ParsedInsightDto parseAiResponse(String rawJsonResponse) {
        try{
            int startIndex = rawJsonResponse.indexOf("{");
            int endIndex = rawJsonResponse.lastIndexOf("}");

            // AI가 JSON 포맷을 주지 않았을 때의 완벽한 방어
            if (startIndex == -1 || endIndex == -1) {
                log.error("AI 응답에서 JSON 포맷을 찾을 수 없습니다. 원본: {}", rawJsonResponse);
                throw new BaseException(ResultCode.AI_PARSE_ERROR);
            }

            String cleanedJson = rawJsonResponse.substring(startIndex, endIndex + 1);

            return objectMapper.readValue(cleanedJson,ParsedInsightDto.class);

        } catch (Exception e) {
            log.error("주간 칭호 JSON 파싱 실패. 원본: {}", rawJsonResponse, e);
            throw new BaseException(ResultCode.AI_PARSE_ERROR);
        }
    }

    /**
     * 전체 일기 날짜 기반 streak 재계산 (생성-과거날짜 / 삭제 공통 로직)
     *
     * @param memberId 회원 PK
     * @param insight  갱신할 MemberInsight 엔티티
     */
    private void syncStreak(Long memberId, MemberInsight insight) {
        List<LocalDate> dates = diaryQueryService.getDistinctDiaryDates(memberId);

        if (dates.isEmpty()) {
            insight.updateStreak(0, null);
            return;
        }

        LocalDate lastDate = dates.get(0);
        LocalDate today = LocalDate.now();

        int currentStreak = isStreakActive(lastDate, today) ? 1 : 0;
        if (currentStreak == 1) {
            // 최신 날짜(0번)부터 과거로 내려가면서 며칠 연속인지 셉니다.
            for (int i = 0; i < dates.size() - 1; i++) {
                // 이전 날짜(i+1)의 '다음 날'이 현재 날짜(i)와 같으면 연속!
                if (dates.get(i + 1).plusDays(1).equals(dates.get(i))) {
                    currentStreak++;
                } else {
                    break;
                }
            }
        }

        insight.updateStreak(currentStreak, lastDate);
    }


    /**
     * streak가 활성 상태인지 확인한다.
     * 마지막 일기 날짜가 오늘 또는 어제인 경우에만 활성으로 판단한다.
     *
     * @param lastDiaryDate 마지막 일기 작성 날짜
     * @param today         오늘 날짜
     * @return 활성 여부
     */
    private boolean isStreakActive(LocalDate lastDiaryDate, LocalDate today) {
        if (lastDiaryDate == null) return false;
        return lastDiaryDate.equals(today) || lastDiaryDate.equals(today.minusDays(1));
    }

    /**
     * 두 날짜 중 더 최근 날짜를 반환한다.
     *
     * @param a 날짜 A
     * @param b 날짜 B
     * @return 더 최근 날짜
     */
    private LocalDate maxDate(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private record ParsedInsightDto(String weeklyIdentity, String weeklyKeyword) {}


}
