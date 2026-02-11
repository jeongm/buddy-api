package com.buddy.buddyapi.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {
    boolean existsByMemberAndProvider(Member member, Provider provider);

}
