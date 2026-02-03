package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"buddyCharacter"})
    @Query("SELECT m FROM Member m WHERE m.memberSeq = :memberSeq")
    Optional<Member> findByIdWithCharacter(@Param("memberSeq") Long memberSeq);

    default Member findByIdOrThrow(Long memberSeq) {
        return findById(memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
    }

}
