package com.buddy.buddyapi.domain.character;

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
    private Long characterSeq;

    @Column(unique = true, nullable = false, length = 100)
    private String name;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String personality;
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "avatar_url", nullable = false, columnDefinition = "TEXT")
    private String avatarUrl = "/";

    @Builder
    public BuddyCharacter(String name, String personality, String description, String avatarUrl) {
        this.name = name;
        this.personality = personality;
        this.description = description;
        this.avatarUrl = avatarUrl;
    }
}
