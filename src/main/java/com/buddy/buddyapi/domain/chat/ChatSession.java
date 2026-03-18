package com.buddy.buddyapi.domain.chat;

import com.buddy.buddyapi.domain.character.BuddyCharacter;
import com.buddy.buddyapi.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_session")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private BuddyCharacter buddyCharacter;

    @Column(name = "is_ended", nullable = false)
    private boolean isEnded = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deletion_notified_at")
    private LocalDateTime deletionNotifiedAt; // 기본값은 null (알림 안 보냄)

    @Builder
    public ChatSession(Member member, BuddyCharacter buddyCharacter) {
        this.member = member;
        this.buddyCharacter = buddyCharacter;
    }

    public void endSession() {
        if(this.isEnded) {
            return;
        }
        this.isEnded = true;
    }

    // 알림 발송 시점을 기록하는 메서드
    public void markDeletionNotified() {
        this.deletionNotifiedAt = LocalDateTime.now();
    }

    // 알림을 이미 보냈는지 확인하는 편의 메서드
    public boolean isDeletionNotified() {
        return this.deletionNotifiedAt != null;
    }

}
