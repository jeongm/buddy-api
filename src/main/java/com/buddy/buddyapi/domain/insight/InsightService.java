package com.buddy.buddyapi.domain.insight;

import com.buddy.buddyapi.domain.ai.AiService;
import com.buddy.buddyapi.domain.diary.DiaryRepository;
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
public class InsightService {

    private final InsightRepository insightRepository;
    private final DiaryRepository diaryRepository;
    private final MemberService memberService;

    private final AiService aiService;

    private final ObjectMapper objectMapper;


    /**
     * 주간 아이덴티티(칭호) 조회 및 생성 (Lazy Evaluation)
     */
    @Transactional
    public WeeklyIdentityResponse getOrUpdateWeeklyInsight(Long memberId) {
        Member member = memberService.getMemberById(memberId);

        // 유저의 통계 테이블 조회 (없으면 null)
        MemberInsight insight = insightRepository.findByMember_MemberId(memberId)
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
        List<String> diaryContents = diaryRepository.findDiaryContentsByMemberAndDateRange(memberId, startOfLastWeek, endOfLastWeek);
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

        // 1. 지난주 월~일 날짜 계산
        LocalDate lastMonday = LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastSunday = LocalDate.now().minusWeeks(1).with(DayOfWeek.SUNDAY);

        // 2. QueryDSL 레포지토리 호출 (5개 제한)
        return diaryRepository.findTopTagsByMemberAndDateRange(memberId, lastMonday, lastSunday, 5);
    }

    /**
     * 캐시 유효성 검사
     */
    private boolean isCacheValid(MemberInsight insight) {
        // 이번 주 월요일 00:00:00 (시간 비교를 위해 LocalDateTime)
        LocalDateTime startOfThisWeek = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        return insight != null && insight.getUpdatedAt() != null && insight.getUpdatedAt().isAfter(startOfThisWeek);
    }

    /**
     * 엔티티 업데이트 및 저장 (그리고 DTO 변환까지 한 큐에!)
     */
    private WeeklyIdentityResponse updateAndSaveInsight(MemberInsight insight, String identity, String keyword) {
        insight.updateWeeklyInsight(identity, keyword);
        insightRepository.save(insight);
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

    private record ParsedInsightDto(String weeklyIdentity, String weeklyKeyword) {}


}
