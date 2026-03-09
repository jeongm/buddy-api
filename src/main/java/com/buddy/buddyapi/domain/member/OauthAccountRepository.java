package com.buddy.buddyapi.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {
    boolean existsByMemberAndProvider(Member member, Provider provider);

    List<OauthAccount> findByMember_MemberSeq(Long memberSeq);

}
