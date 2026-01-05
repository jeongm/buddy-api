package com.buddy.buddyapi.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_session")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_seq", nullable = false)
    private BuddyCharacter buddyCharacter;

    @Column(name = "is_ended", nullable = false)
    private boolean isEnded = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


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

}
