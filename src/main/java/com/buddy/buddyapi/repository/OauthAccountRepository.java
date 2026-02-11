package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.domain.Provider;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.entity.OauthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {
    boolean existsByMemberAndProvider(Member member, Provider provider);

}
