package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.entity.BuddyCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuddyCharacterRepository extends JpaRepository<BuddyCharacter, Integer> {
}
