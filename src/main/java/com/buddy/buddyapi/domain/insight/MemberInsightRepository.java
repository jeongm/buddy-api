package com.buddy.buddyapi.domain.insight;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberInsightRepository extends JpaRepository<MemberInsight, Long> {
    Optional<MemberInsight> findByMember_MemberId(Long memberId);
}
