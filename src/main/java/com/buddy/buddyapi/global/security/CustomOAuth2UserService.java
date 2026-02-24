package com.buddy.buddyapi.global.security;

import com.buddy.buddyapi.domain.member.Provider;
import com.buddy.buddyapi.domain.member.Member;
import com.buddy.buddyapi.domain.member.OauthAccount;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.domain.member.MemberRepository;
import com.buddy.buddyapi.domain.member.OauthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final OauthAccountRepository oauthAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. 기본 DefaultOAuth2UserService를 통해 유저 정보를 가져옴
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 현재 로그인 진행 중인 서비스 구분 (google, naver 등)
        String registrationID = userRequest.getClientRegistration().getRegistrationId();

        // 3. OAuth2 로그인 진행 시 키가 되는 필드값 (구글-'sub')
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 4. OAuth서버에서 준 json을 우리가 만든 OAuthAttributes로 변환
        OAuthAttributes attributes = OAuthAttributes.of(registrationID, userNameAttributeName,
                oAuth2User.getAttributes());

        // 5. 회원 가입 여부 확인 및 연동 확인
        Member member = processUserAuthentication(attributes);

        // 시큐리티 반환 시: 어떤 키를 봐야 할지 알려줌
        return CustomUserDetails.of(member, attributes.attributes());

    }

    private Member processUserAuthentication(OAuthAttributes attributes) {

        Optional<Member> memberOptional = memberRepository.findByEmail(attributes.email());

        if (memberOptional.isPresent()) {
            Member member = memberOptional.get();

            // 해당 소셜로 이미 연동되어 있는지 확인
            Provider provider = Provider.from(attributes.registrationId());
            boolean isLinked = oauthAccountRepository.existsByMemberAndProvider(member, provider);

            if (!isLinked) {
                // 여기서 예외를 던지면 AuthenticationFailureHandler 가 낚아채서
                // "이미 가입된 이메일입니다. 연동하시겠습니까?" 페이지로 리다이렉트 시킴
                String errorMessage = String.format("%s:%s:%s:%s",
                        ResultCode.ALREADY_SIGNED_UP_EMAIL.name(),
                        attributes.email(),
                        attributes.registrationId(),
                        attributes.oauthId());

                throw new OAuth2AuthenticationException(new OAuth2Error(errorMessage));
            }

            return member;

        }

        return createNewMember(attributes);

    }

    @Transactional
    private Member createNewMember(OAuthAttributes attributes) {

        Member newMember = attributes.toEntity();

        Member savedMember = memberRepository.save(newMember);

        OauthAccount oauthAccount = OauthAccount.builder()
                .provider(Provider.from(attributes.registrationId()))
                .oauthId(attributes.oauthId())
                .member(savedMember)
                .build();

        oauthAccountRepository.save(oauthAccount);

        return savedMember;

    }

}
