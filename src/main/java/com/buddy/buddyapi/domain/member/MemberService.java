package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.domain.character.BuddyCharacter;
import com.buddy.buddyapi.domain.character.BuddyCharacterRepository;
import com.buddy.buddyapi.domain.member.dto.*;
import com.buddy.buddyapi.domain.member.dto.MemberSeqResponse;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final BuddyCharacterRepository characterRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 일반 회원가입 처리
     * @param request 회원가입 정보 (이메일, 비밀번호, 닉네임, 캐릭터 번호 등)
     * @return memberResponse 가입 완료된 회원의 정보 DTO
     * @throws BaseException 이미 존재하는 이메일이거나 캐릭터가 없을 경우 발생
     */
    @Transactional
    public MemberSeqResponse registerMember(MemberRegisterRequest request) {

        // 이메일 인증 여부 확인
        String isVerified = redisTemplate.opsForValue().get("verified:" + request.getEmail());

        if (!"true".equals(isVerified)) {
            // 인증 안 된 상태면 가입 거부 (401 Unauthorized 또는 400 Bad Request)
            throw new BaseException(ResultCode.UNAUTHORIZED);
        }

        redisTemplate.delete("verified:" + request.getEmail());

        checkEmailDuplicate(request.getEmail());

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 초기 캐릭터 조회 및 유효성 검사
        // DB에 1번 캐릭터가 반드시 존재해야 한다는 강력한 전제가 필요
        Long charSeq = request.getCharacterSeq() != null ? request.getCharacterSeq() : 1;

        BuddyCharacter selectedCharacter = characterRepository.findById(charSeq)
                .orElseThrow(()-> new BaseException(ResultCode.CHARACTER_NOT_FOUND));

        Member newMember = Member.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .buddyCharacter(selectedCharacter)
                .build();

        Member savedMember = memberRepository.save(newMember);

        return MemberSeqResponse.from(savedMember);
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
        // 1. 유저 조회
        Member member = memberRepository.findByIdOrThrow(memberSeq);

        // 2. 기존 비밀번호 확인 (Spring Security의 matches 사용)
        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new BaseException(ResultCode.CURRENT_PASSWORD_MISMATCH);
        }

        // 3. 새 비밀번호 암호화 및 저장
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
        Member member = memberRepository.findByIdOrThrow(memberSeq);
        return MemberResponse.from(member);
    }

    /**
     * 회원이 사용할 버디 캐릭터를 변경합니다.
     *
     * @param memberSeq 현재 로그인한 회원 정보
     * @param request    변경하고자 하는 캐릭터의 식별자가 담긴 DTO
     * @return 캐릭터가 변경된 후의 회원 정보 응답 DTO
     * @throws BaseException 존재하지 않는 캐릭터 ID이거나 회원을 찾을 수 없을 경우 발생
     */
    @Transactional
    public MemberResponse changeMyCharacter(Long memberSeq, CharacterChangeRequest request) {
        BuddyCharacter newCharacter = characterRepository.findById(request.characterSeq())
                .orElseThrow(() -> new BaseException(ResultCode.CHARACTER_NOT_FOUND));

        Member member = memberRepository.findByIdOrThrow(memberSeq);

        member.changeCharacter(newCharacter);

        return MemberResponse.from(member);
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

    public void checkEmailDuplicate(String email) {
        if(memberRepository.existsByEmail(email)) {
            throw new BaseException(ResultCode.EMAIL_DUPLICATED);
        }
    }
}
