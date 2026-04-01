package com.buddy.buddyapi.domain.insight;

import com.buddy.buddyapi.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_insight")
public class MemberInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insight_id")
    private Long insightId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", unique = true, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    // ==========================================
    // [리포트/통계 영역] - 추후 월간 기록 등이 생길 시 1:N으로 분리하도록 한다.
    // ==========================================

    // 일주일에 한번 업데이트되며 이전 주의 기록을 바탕으로 함 이전주의 기록이 없을 시 null값 저장
    @Column(name = "weekly_identity", length = 50)
    private String weeklyIdentity;

    @Column(name = "weekly_keyword", length = 20)
    private String weeklyKeyword;

    @Column(name = "weekly_updated_at")
    private LocalDateTime weeklyUpdatedAt;

    // ==========================================
    // [연속 기록 (Streak) 영역] - 실시간 상태
    // ==========================================

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "best_streak", nullable = false)
    private int bestStreak;

    @Column(name = "last_diary_date")
    private LocalDate lastDiaryDate;

    @Builder
    public MemberInsight(Member member, String weeklyIdentity, String weeklyKeyword, LocalDateTime weeklyUpdatedAt) {
        this.member = member;
        this.weeklyIdentity = weeklyIdentity;
        this.weeklyKeyword = weeklyKeyword;
        this.weeklyUpdatedAt = weeklyUpdatedAt;
    }

    // 주간 칭호 업데이트 메서드
    public void updateWeeklyInsight(String newIdentity, String newKeyword) {
        this.weeklyIdentity = newIdentity;
        this.weeklyKeyword = newKeyword;
        this.weeklyUpdatedAt = LocalDateTime.now();
    }


    public void updateStreak(int newCurrentStreak, LocalDate lastDiaryDate) {
        this.currentStreak = newCurrentStreak;
        this.bestStreak = Math.max(this.bestStreak, newCurrentStreak);
        this.lastDiaryDate = lastDiaryDate;
    }
}
