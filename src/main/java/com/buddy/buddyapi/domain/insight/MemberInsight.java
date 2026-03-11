package com.buddy.buddyapi.domain.insight;

import com.buddy.buddyapi.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_insight")
public class MemberInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insight_seq")
    private Long insightSeq;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    // 일주일에 한번 업데이트되며 이전 주의 기록을 바탕으로 함 이전주의 기록이 없을 시 null값 저장
    @Column(name = "weekly_identity", length = 50)
    private String weeklyIdentity;

    @Column(name = "weekly_keyword", length = 20)
    private String weeklyKeyword;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public MemberInsight(Member member, String weeklyIdentity, String weeklyTopTag, LocalDateTime updatedAt) {
        this.member = member;
        this.weeklyIdentity = weeklyIdentity;
        this.weeklyKeyword = weeklyTopTag;
        this.updatedAt = updatedAt;
    }

    // 주간 칭호 업데이트 메서드
    public void updateWeeklyInsight(String newIdentity, String newTopTag) {
        this.weeklyIdentity = newIdentity;
        this.weeklyKeyword = newTopTag;
        this.updatedAt = LocalDateTime.now();
    }
}
