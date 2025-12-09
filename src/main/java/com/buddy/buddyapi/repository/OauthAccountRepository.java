package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.domain.Provider;
import com.buddy.buddyapi.entity.OauthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, Long> {
    Optional<OauthAccount> findByProviderAndOauthId(Provider provider, String oauthId);
//    boolean existsByUser_UserSeqAndOauthProvider(Long userSeq, Provider provider);

}
