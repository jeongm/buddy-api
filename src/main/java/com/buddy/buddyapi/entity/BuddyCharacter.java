package com.buddy.buddyapi.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "buddy_character")
@Entity
public class BuddyCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_seq")
    private Integer characterSeq;

    @Column(unique = true, nullable = false, length = 100)
    private String name;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String personality;
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "avatar_url", nullable = false, columnDefinition = "TEXT")
    private String avatarUrl; // TODO 기본값 있이할까 없이할까

    @Builder
    public BuddyCharacter(String name, String personality, String description, String avatarUrl) {
        this.name = name;
        this.personality = personality;
        this.description = description;
        this.avatarUrl = avatarUrl;
    }
}
