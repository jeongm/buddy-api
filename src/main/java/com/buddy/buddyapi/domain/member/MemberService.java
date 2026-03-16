package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.domain.auth.dto.AuthDto;
import com.buddy.buddyapi.domain.character.BuddyCharacter;
import com.buddy.buddyapi.domain.character.BuddyCharacterRepository;
import com.buddy.buddyapi.domain.member.dto.*;
import com.buddy.buddyapi.domain.member.event.MemberWithdrawEvent;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final BuddyCharacterRepository characterRepository;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 일반(이메일) 회원가입을 처리하고 새로운 회원을 생성합니다.
     * @param request 회원가입 정보 (이메일, 비밀번호, 닉네임, 캐릭터 번호 등)
     * @param encodedPassword 시큐리티를 통해 암호화된 비밀번호
     * @return 가입 완료된 회원의 식별자(PK) 정보를 담은 DTO
     * @throws BaseException 이미 존재하는 이메일이거나, 선택한 캐릭터가 존재하지 않을 경우 발생
     */
    @Transactional
    public Member registerLocalMember(AuthDto.SignUpRequest request, String encodedPassword) {

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

        return memberRepository.save(newMember);
    }

    /**
     * 신규 소셜 로그인 유저를 데이터베이스에 저장합니다.
     * @param email 소셜 제공자로부터 전달받은 이메일
     * @param nickname 소셜 제공자로부터 전달받은 닉네임
     * @return 가입 완료된 회원 엔티티 (Member)
     */
    @Transactional
    public Member registerSocialMember(String email, String nickname) {
        return memberRepository.save(
                Member.builder()
                        .email(email)
                        .nickname(nickname)
                        .build()
        );
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
     * 현재 비밀번호가 맞는지 검증합니다.
     */
    @Transactional(readOnly = true)
    public void verifyPassword(Long memberSeq, String rawPassword) {
        Member member = memberRepository.findByIdOrThrow(memberSeq);

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new BaseException(ResultCode.CURRENT_PASSWORD_MISMATCH);
        }
    }

    /**
     * 회원의 비밀번호를 변경합니다.
     * @param memberSeq 비밀번호를 변경할 회원의 고유 식별자
     * @param currentPassword 현재비밀번호
     * @param newPassword 새 비밀번호를 담은 DTO
     * @throws BaseException 기존 비밀번호가 일치하지 않거나 유저가 없을 경우 발생
     */
    @Transactional
    public void updateMemberPassword(Long memberSeq, String currentPassword, String newPassword) {
        verifyPassword(memberSeq,currentPassword);

        Member member = memberRepository.findByIdOrThrow(memberSeq);
        String encodedNewPassword = passwordEncoder.encode(newPassword);
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

    /**
     * [회원 탈퇴] 소셜 연결을 끊고, 토큰을 파기하며, DB에서 회원을 삭제합니다.
     * @param memberSeq 현재 로그인한 회원 정보
     */
    @Transactional
    public void deleteMember(Long memberSeq) {

        eventPublisher.publishEvent(new MemberWithdrawEvent(memberSeq));

        // 3. MemberService에게 지시: "이제 우리 DB에서 진짜로 유저 정보 지워!"
        memberRepository.deleteById(memberSeq);
    }
    /**
     * 이메일 중복 체크
     * @param email 중복체크할 이메일
     */
    @Transactional(readOnly = true)
    public void checkEmailDuplicate(String email) {
        if(memberRepository.existsByEmail(email)) {
            throw new BaseException(ResultCode.EMAIL_DUPLICATED);
        }
    }

    @Transactional
    public void updatePushToken(Long memberSeq, String pushToken) {
        Member member = memberRepository.findByIdOrThrow(memberSeq);

        // 2. 토큰 업데이트 (더티 체킹으로 자동 UPDATE 쿼리 발생)
        member.updatePushToken(pushToken);
    }
}
