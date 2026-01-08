package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.request.MemberLoginRequest;
import com.buddy.buddyapi.dto.request.MemberRegisterRequest;
import com.buddy.buddyapi.dto.request.UpdateNicknameRequest;
import com.buddy.buddyapi.dto.request.UpdatePasswordRequest;
import com.buddy.buddyapi.dto.response.LoginResponse;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.dto.response.UpdateNicknameResponse;
import com.buddy.buddyapi.entity.BuddyCharacter;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.global.config.JwtTokenProvider;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.repository.BuddyCharacterRepository;
import com.buddy.buddyapi.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final BuddyCharacterRepository characterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    //  -- auth관련 로직 --

    /**
     * 일반 회원가입 처리
     * @param request 회원가입 정보 (이메일, 비밀번호, 닉네임, 캐릭터 번호 등)
     * @return memberResponse 가입 완료된 회원의 정보 DTO
     * @throws BaseException 이미 존재하는 이메일이거나 캐릭터가 없을 경우 발생
     */
    @Override
    @Transactional
    public MemberResponse registerMember(MemberRegisterRequest request) {
        // 1. 중복 검사 (이메일, 닉네임)
        if(memberRepository.existsByEmail(request.getEmail())) {
            throw new BaseException(ResultCode.EMAIL_DUPLICATED);
        }

        // 2. 비밀번호 암호화 (BCrypt 등)
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. 초기 캐릭터 조회 및 유효성 검사
        Long charSeq = request.getCharacterSeq() != null ? request.getCharacterSeq() : 1;

        BuddyCharacter selectedCharacter = characterRepository.findById(charSeq)
                .orElseThrow(()-> new BaseException(ResultCode.CHARACTER_NOT_FOUND));

        // 3. Request DTO를 Entity로 변환
        Member newMember = Member.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getNickname())
                .buddyCharacter(selectedCharacter)
                .build();
        // 4. memberRepository.save(newMember)
        Member savedmember = memberRepository.save(newMember);
        // jpa는 영속성 컨텍스트라서 그냥 newMember그대로 반환해도 되지 않나? -> 그래도 안전성을 위해 그냥 이렇게 하기로 함

        // 5. 저장된 Entity를 Response DTO로 변환하여 반환
        return MemberResponse.from(savedmember);
    }

    /**
     * 이메일과 비밀번호를 기반으로 로그인을 처리하고 JWT 토큰을 발급합니다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @return 액세스 토큰, 리프레시 토큰 및 회원 정보를 포함한 응답 DTO
     * @throws BaseException 유저를 찾을 수 없거나 비밀번호가 일치하지 않을 경우 발생
     */
    @Override
    @Transactional(readOnly = true)
    public LoginResponse localLoginMember(MemberLoginRequest request) {
        // 1. userRepository.findByEmail()
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
        // 2. 암호화된 비밀번호 매칭 검사
        if(!passwordEncoder.matches(request.getPassword(), member.getPassword())){
            throw new BaseException(ResultCode.INVALID_CREDENTIALS);
        }

        // 3. JWT 토큰 생성 (JwtTokenProvider 사용)
        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail());

        // TODO 3-2 Refresh Token을 DB나 Redis에 저장하여 로그아웃/재발급에 대비

        // 4. 성공 시 LoginResponse 반환
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .member(MemberResponse.from(member))
                .build();
    }

    // -- member관련 로직 --

    /**
     * 회원의 닉네임을 변경합니다.
     * @param memberSeq 변경할 회원의 고유 식별자
     * @param request 새로운 닉네임 정보를 담은 DTO
     * @return 변경된 닉네임 결과 DTO
     * @throws BaseException 해당 회원이 존재하지 않을 경우 발생
     */
    @Override
    @Transactional
    public UpdateNicknameResponse updateNickName(Long memberSeq, UpdateNicknameRequest request) {

        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

        member.updateNickname(request.nickname());

        return new UpdateNicknameResponse(member.getNickname());
    }

    /**
     * 회원의 비밀번호를 변경합니다.
     * @param memberSeq 비밀번호를 변경할 회원의 고유 식별자
     * @param request 현재비밀번호 및 새 비밀번호를 담은 DTO
     * @throws BaseException 기존 비밀번호가 일치하지 않거나 유저가 없을 경우 발생
     */
    @Override
    @Transactional
    public void updateMemberPassword(Long memberSeq, UpdatePasswordRequest request) {
        // 1. 유저 조회
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() ->  new BaseException(ResultCode.USER_NOT_FOUND));

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
    @Override
    @Transactional(readOnly = true)
    public MemberResponse getUserDetails(Long memberSeq) {
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
        return MemberResponse.from(member);
    }

}
