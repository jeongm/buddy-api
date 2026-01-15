package com.buddy.buddyapi.service;

import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.global.config.CustomUserDetails;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String memberSeqStr) throws UsernameNotFoundException {
        Long memberSeq = Long.parseLong(memberSeqStr);
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

        return CustomUserDetails.from(member);
    }
}
