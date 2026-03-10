package com.buddy.buddyapi.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {
    boolean existsByMemberAndProvider(Member member, Provider provider);

    List<OauthAccount> findByMember_MemberSeq(Long memberSeq);

    Optional<OauthAccount> findByMemberAndProvider(Member member, Provider provider);

}
