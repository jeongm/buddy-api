package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
//    boolean existsByName(String name);
    // tagSeq 리스트로 한꺼번에 태그들을 찾아올 때 사용
    List<Tag> findAllByTagSeqIn(List<Long> tagSeqs);
    Optional<Tag> findByName(String name);
}
