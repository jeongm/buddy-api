package com.buddy.buddyapi.domain.insight;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsightRepository extends JpaRepository<MemberInsight, Long> {
    Optional<MemberInsight> findByMember_MemberSeq(Long memberSeq);
}
