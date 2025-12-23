package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.request.MemberLoginRequest;
import com.buddy.buddyapi.dto.request.MemberRegisterRequest;
import com.buddy.buddyapi.dto.request.UpdateNicknameRequest;
import com.buddy.buddyapi.dto.request.UpdatePasswordRequest;
import com.buddy.buddyapi.dto.response.LoginResponse;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.dto.response.UpdateNicknameResponse;

public interface MemberService {

    /**
    * 신규 사용자를 등록 및 초기 캐릭터 설정(회원가입)
    * @param request 회원가입 요청 DTO
    * @return 등록된 사용자 정보 DTO
    */
    MemberResponse registerMember(MemberRegisterRequest request);

    /**
     * 이메일과 비밀번호를 검증하여 사용자를 인증(로그인)
     * @param request 로그인 요청 DTO
     * @return 인증된 사용자 정보 DTO 
     */
    LoginResponse localLoginMember(MemberLoginRequest request);

    //    boolean isNicknameDuplicate(String nickname);

    /**
     * 사용자 닉네임 변경
     * @param nickname 변경할 닉네임
     */
    UpdateNicknameResponse updateNickName(Long memberSeq, UpdateNicknameRequest nickname);

    /**
     * 비밀번호 변경
     * @param request 현재비밀번호 및 새 비밀번호
     */
    void updateMemberPassword(Long memberSeq, UpdatePasswordRequest request);


    /**
     * 특정 사용자 식별자(memberSeq)를 통해 사용자 정보 조회
     * @param memberSeq 사용자 식별자
     * @return 사용자 정보 DTO
     */
    MemberResponse getUserDetails(Long memberSeq);





}
