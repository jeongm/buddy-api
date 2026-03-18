package com.buddy.buddyapi.domain.character.dto;

import com.buddy.buddyapi.domain.character.BuddyCharacter;

public record CharacterResponse(
        Long characterId,
        String name,
        String description,
        String personality,
        String avatarUrl
) {
    public static CharacterResponse from(BuddyCharacter character) {
        return new CharacterResponse(
                character.getCharacterId(),
                character.getName(),
                character.getDescription(),
                character.getPersonality(),
                character.getAvatarUrl()
        );
    }
}
