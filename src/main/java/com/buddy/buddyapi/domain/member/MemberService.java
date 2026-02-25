package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.domain.auth.component.OAuthUserInfo;
import com.buddy.buddyapi.domain.auth.dto.SignUpRequest;
import com.buddy.buddyapi.domain.character.BuddyCharacter;
import com.buddy.buddyapi.domain.character.BuddyCharacterRepository;
import com.buddy.buddyapi.domain.member.dto.*;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final OauthAccountRepository oauthAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final BuddyCharacterRepository characterRepository;

    /**
     * 일반(이메일) 회원가입을 처리하고 새로운 회원을 생성합니다.
     * @param request 회원가입 정보 (이메일, 비밀번호, 닉네임, 캐릭터 번호 등)
     * @param encodedPassword 시큐리티를 통해 암호화된 비밀번호
     * @return 가입 완료된 회원의 식별자(PK) 정보를 담은 DTO
     * @throws BaseException 이미 존재하는 이메일이거나, 선택한 캐릭터가 존재하지 않을 경우 발생
     */
    @Transactional
    public Member registerLocalMember(SignUpRequest request, String encodedPassword) {

        checkEmailDuplicate(request.email());

        BuddyCharacter selectedCharacter = null;
        if (request.characterSeq() != null) {
            selectedCharacter = characterRepository.findById(request.characterSeq())
                    .orElseThrow(() -> new BaseException(ResultCode.CHARACTER_NOT_FOUND));
        }

        Member newMember = Member.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(request.nickname())
                .buddyCharacter(selectedCharacter)
                .build();

        Member savedMember = memberRepository.save(newMember);

        return savedMember;
    }

    /**
     * 신규 소셜 로그인 유저를 데이터베이스에 등록하고 연동 정보를 함께 저장합니다.
     * @param userInfo 소셜 제공자로부터 전달받은 유저 정보 (이메일, 닉네임, 고유 ID 등)
     * @param provider 소셜 제공자 (Google, Kakao, Naver)
     * @return 가입 완료된 회원 엔티티 (Member)
     */
    @Transactional
    public Member registerSocialMember(OAuthUserInfo userInfo, Provider provider) {
        Member newMember = memberRepository.save(
                Member.builder()
                        .email(userInfo.email())
                        .nickname(userInfo.name())
                        .build()
        );

        oauthAccountRepository.save(OauthAccount.builder()
                .provider(provider)
                .oauthId(userInfo.oauthId())
                .member(newMember).build()
        );


        return newMember;
    }

    /**
     * 기존 회원 계정에 새로운 소셜 계정 정보를 연동(추가)합니다.
     * @param member 소셜 계정을 연동할 대상 기존 회원 엔티티
     * @param provider 연동할 소셜 제공자 (Google, Kakao, Naver)
     * @param oauthId 소셜 제공자 측의 고유 식별자 (ID)
     * @throws BaseException 해당 소셜 제공자로 이미 연동된 계정이 존재할 경우 발생
     */
    @Transactional
    public void linkSocialAccount(Member member, Provider provider, String oauthId) {

        if (hasSocialAccount(member, provider)) {
            throw new BaseException(ResultCode.ALREADY_LINKED_ACCOUNT);
        }

        OauthAccount oauthAccount = OauthAccount.builder()
                .provider(provider)
                .oauthId(oauthId)
                .member(member)
                .build();

        oauthAccountRepository.save(oauthAccount);
    }

    /**
     * 특정 회원이 해당 소셜 제공자와 이미 연동되어 있는지 여부를 확인합니다.
     * @param member 확인할 회원 엔티티
     * @param provider 소셜 제공자 (Google, Kakao, Naver)
     * @return 이미 연동되어 있다면 true, 아니면 false 반환
     */
    @Transactional(readOnly = true)
    public boolean hasSocialAccount(Member member, Provider provider) {
        return oauthAccountRepository.existsByMemberAndProvider(member, provider);
    }



    /**
     * 회원의 닉네임을 변경합니다.
     * @param memberSeq 변경할 회원의 고유 식별자
     * @param request 새로운 닉네임 정보를 담은 DTO
     * @return 변경된 닉네임 결과 DTO
     * @throws BaseException 해당 회원이 존재하지 않을 경우 발생
     */
    @Transactional
    public UpdateNicknameResponse updateNickName(Long memberSeq, UpdateNicknameRequest request) {

        Member member = memberRepository.findByIdOrThrow(memberSeq);

        member.updateNickname(request.nickname());

        return new UpdateNicknameResponse(member.getNickname());
    }

    /**
     * 회원의 비밀번호를 변경합니다.
     * @param memberSeq 비밀번호를 변경할 회원의 고유 식별자
     * @param request 현재비밀번호 및 새 비밀번호를 담은 DTO
     * @throws BaseException 기존 비밀번호가 일치하지 않거나 유저가 없을 경우 발생
     */
    @Transactional
    public void updateMemberPassword(Long memberSeq, UpdatePasswordRequest request) {

        Member member = memberRepository.findByIdOrThrow(memberSeq);

        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new BaseException(ResultCode.CURRENT_PASSWORD_MISMATCH);
        }

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        member.updatePassword(encodedNewPassword);
    }

    /**
     * 내 정보(상세 프로필)를 조회합니다.
     *
     * @param memberSeq 조회할 회원의 고유 식별자
     * @return 회원의 이메일, 닉네임, 캐릭터 정보 등을 포함한 DTO
     * @throws BaseException 해당 회원이 존재하지 않을 경우 발생
     */
    @Transactional(readOnly = true)
    public MemberResponse getUserDetails(Long memberSeq) {
        Member member = memberRepository.findByIdWithCharacter(memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
        return MemberResponse.from(member);
    }

    /**
     * 회원이 사용할 버디 캐릭터를 변경합니다.
     *
     * @param memberSeq 현재 로그인한 회원 정보
     * @param request    변경하고자 하는 캐릭터의 식별자가 담긴 DTO
     * @throws BaseException 존재하지 않는 캐릭터 ID이거나 회원을 찾을 수 없을 경우 발생
     */
    @Transactional
    public void changeMyCharacter(Long memberSeq, CharacterChangeRequest request) {

        Member member = memberRepository.findByIdOrThrow(memberSeq);

        //캐릭터는 가짜 프록시 객체로 가져옴 (SELECT 발생 안 함)
        // DB에 가지 않고, id값만 가진 껍데기 객체를 만듭니다.
        BuddyCharacter newCharacter = characterRepository.getReferenceById(request.characterSeq());

        member.changeCharacter(newCharacter);

    }

    /**
     * 현재 사용 중인 버디 캐릭터의 별명(애칭)을 변경합니다.
     *
     * @param memberSeq  현재 로그인한 회원 정보
     * @param newName 새로 설정할 캐릭터의 별명
     * @throws BaseException 회원을 찾을 수 없을 경우 발생
     */
    @Transactional
    public void updateCharacterNickname(Long memberSeq, String newName) {
        Member member = memberRepository.findByIdOrThrow(memberSeq);
        member.updateCharacterNickname(newName);
        memberRepository.save(member);
    }

    @Transactional
    public void deleteMember(Long memberSeq) {
        memberRepository.deleteById(memberSeq);
    }

    @Transactional(readOnly = true)
    public void checkEmailDuplicate(String email) {
        if(memberRepository.existsByEmail(email)) {
            throw new BaseException(ResultCode.EMAIL_DUPLICATED);
        }
    }
}
