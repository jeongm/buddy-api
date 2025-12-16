package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.UpdatePasswordRequest;
import com.buddy.buddyapi.dto.UserLoginRequest;
import com.buddy.buddyapi.dto.UserRegisterRequest;
import com.buddy.buddyapi.dto.UserResponse;
import com.buddy.buddyapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


public interface UserService {

    /**
    * 신규 사용자를 등록 및 초기 캐릭터 설정(회원가입)
    * @param request 회원가입 요청 DTO
    * @return 등록된 사용자 정보 DTO
    */
    UserResponse registerUser(UserRegisterRequest request);

    /**
     * 이메일과 비밀번호를 검증하여 사용자를 인증(로그인)
     * @param request 로그인 요청 DTO
     * @return 인증된 사용자 정보 DTO 
     */
    UserResponse localLoginUser(UserLoginRequest request);

    /**
     * 특정 사용자 식별자(userSeq)를 통해 사용자 정보 조회
     * @param userSeq 사용자 식별자
     * @return 사용자 정보 DTO
     */
    UserResponse getUserDetails(Long userSeq);

    /**
     * 사용자 닉네임의 중복 여부를 확인
     * @param nickname 확인할 닉네임
     * @return 중복 여부 (true: 중복, false: 사용 가능)
     */
//    boolean isNicknameDuplicate(String nickname);

    /**
     * 사용자 닉네임 변경
     * @param nickname 변경할 닉네임
     */
    void updateNickName(String nickname);

    /**
     * 비밀번호 변경
     * @param request 현재비밀번호 및 새 비밀번호
     */
    void updateUserPassword(UpdatePasswordRequest request);







}
