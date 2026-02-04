package com.buddy.buddyapi.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "diary")
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_seq")
    private Long diarySeq;

    @Column(length = 100)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_seq")
    private ChatSession chatSession;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DiaryTag> diaryTags = new ArrayList<>();

    @Builder
    public Diary(String title, String content, LocalDate diaryDate, String imageUrl, Member member, ChatSession chatSession) {
        this.title = title;
        this.content = content;
        this.diaryDate = diaryDate;
        this.imageUrl = imageUrl;
        this.member = member;
        this.chatSession = chatSession;
    }

    public boolean isAiGenerated() {
        return this.chatSession != null;
    }

    public void updateDiary(String title, String content, LocalDate diaryDate, String imageUrl) {
        this.title = title;
        this.content = content;
        this.diaryDate = diaryDate;
        this.imageUrl = imageUrl;
    }

    public void addTag(Tag tag) {
        DiaryTag diaryTag = DiaryTag.builder()
                .diary(this)
                .tag(tag)
                .build();
        this.diaryTags.add(diaryTag);
    }

    // 다중 추가 메서드 (리스트를 받아서 단일 메서드들을 호출)
    public void addTags(List<Tag> tags) {
        if (tags != null) {
            tags.forEach(this::addTag); // 위에서 만든 addTag를 재사용
        }
    }


    public void updateTags(List<Tag> newTags) {
        if (newTags == null || newTags.isEmpty()) {
            this.diaryTags.clear();
            return;
        }

        // 기존 태그들중에 새 리스트에 없는 애 삭제
        this.diaryTags.removeIf(diaryTag ->
                !newTags.contains(diaryTag.getTag())
        );

        // 새 리스트의 태그들 중 기존 태그들에 없는 것들만 추가
        List<Tag> currentTags = this.diaryTags.stream()
                .map(DiaryTag::getTag)
                .toList();

        // 이미 있는건 중복 삽입 안하고 없는 놈들만 새로 생성해서 추가
        newTags.stream()
                .filter(tag -> !currentTags.contains(tag))
                .forEach(this::addTag);
    }



}
