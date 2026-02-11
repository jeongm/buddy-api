package com.buddy.buddyapi.domain.chat;

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
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_seq")
    private Long messageSeq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_seq", nullable = false)
    private ChatSession chatSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderRole role; // USER 또는 ASSISTANT(AI)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(ChatSession chatSession, SenderRole role, String content) {
        this.chatSession = chatSession;
        this.role = role;
        this.content = content;
    }
}