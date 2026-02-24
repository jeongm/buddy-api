package com.buddy.buddyapi.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long>, TagRepositoryCustom{
    Optional<Tag> findByName(String name);
    List<Tag> findByNameIn(List<String> names);
}
