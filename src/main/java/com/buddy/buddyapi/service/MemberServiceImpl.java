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
import com.buddy.buddyapi.repository.OauthAccountRepository;
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
    private final OauthAccountRepository oauthAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    //  -- auth관련 로직 --

    /**
     * 일반회원가입
     * @param request 회원가입 요청 DTO
     * @return memberResponse
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
        Integer charSeq = request.getCharacterSeq() != null ? request.getCharacterSeq() : 1;

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
     * 일반 로그인
     * @param request 로그인 요청 DTO
     * @return
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

    @Override
    @Transactional
    public UpdateNicknameResponse updateNickName(Long memberSeq, UpdateNicknameRequest request) {

        // 2. 유저 조회
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

        // 2. 닉네임 중복 체크 (중복체크할필요없음)
//        if (memberRepository.existsByNickname(nickname)) {
//            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
//        }

        // 3. 변경 (Dirty Checking에 의해 자동 반영)
        member.updateNickname(request.getNickname());

        return new UpdateNicknameResponse(member.getNickname());
    }

    @Override
    @Transactional
    public void updateMemberPassword(Long memberSeq, UpdatePasswordRequest request) {
        // 1. 유저 조회
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() ->  new BaseException(ResultCode.USER_NOT_FOUND));

        // 2. 기존 비밀번호 확인 (Spring Security의 matches 사용)
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new BaseException(ResultCode.CURRENT_PASSWORD_MISMATCH);
        }

        // 3. 새 비밀번호 암호화 및 저장
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        member.updatePassword(encodedNewPassword);
    }

    /**
     * 내 정보 조회
     * @param memberSeq 사용자 식별자
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public MemberResponse getUserDetails(Long memberSeq) {
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));
        return MemberResponse.from(member);
    }

}
