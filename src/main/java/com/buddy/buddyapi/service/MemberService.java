package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.request.UpdateNicknameRequest;
import com.buddy.buddyapi.dto.request.UpdatePasswordRequest;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.dto.response.UpdateNicknameResponse;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;


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

}
