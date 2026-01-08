package com.buddy.buddyapi.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DiaryTag> diaryTags = new ArrayList<>();

    @Builder
    public Diary(String title, String content, String imageUrl, Member member) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.member = member;
    }

    public void updateDiary(String title, String content, String imageUrl) {
        this.title = title;
        this.content = content;
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
        // 1. 기존 태그 리스트를 비움(orphanRemoval = true 설정 덕분에 DB에서도 삭제됨)
        this.diaryTags.clear();

        // 2. 새로운 태그들 추가
        addTags(newTags);
    }



}
