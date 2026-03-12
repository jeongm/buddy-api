package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.domain.auth.dto.AuthDto;
import com.buddy.buddyapi.domain.auth.enums.AuthStatus;
import com.buddy.buddyapi.domain.auth.enums.EmailPurpose;
import com.buddy.buddyapi.domain.member.*;
import com.buddy.buddyapi.domain.member.dto.MemberResponse;
import com.buddy.buddyapi.global.security.JwtTokenProvider;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public AuthDto.LoginResponse signup(AuthDto.SignUpRequest request) {
        // 이메일 인증 확인
        String tokenKey = "email_token:SIGNUP:" + request.email();
        String savedToken = redisTemplate.opsForValue().getAndDelete(tokenKey);

        if (savedToken == null || !savedToken.equals(request.verificationToken())) {
            throw new BaseException(ResultCode.UNAUTHORIZED_EMAIL_VERIFICATION); // "이메일 인증이 만료되었거나 올바르지 않습니다."
        }

        memberService.checkEmailDuplicate(request.email());

        String encodedPassword = passwordEncoder.encode(request.password());

        Member newMember = memberService.registerLocalMember(request, encodedPassword);

        // 회원 가입 완료 시 자동 로그인
        return buildAuthResponse(newMember, AuthStatus.SUCCESS);
    }

    /**
     * [일반 로그인] 이메일과 비밀번호를 기반으로 로그인을 처리합니다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @return 성공 상태(SUCCESS), 액세스 토큰, 리프레시 토큰, 회원 정보를 포함한 응답 DTO
     * @throws BaseException 유저를 찾을 수 없거나 비밀번호가 일치하지 않을 경우 발생
     */
    @Transactional
    public AuthDto.LoginResponse localLogin(AuthDto.EmailLoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BaseException(ResultCode.INVALID_CREDENTIALS);
        }

        return buildAuthResponse(member, AuthStatus.SUCCESS);
    }

    /**
     * [토큰 재발급] 만료된 액세스 토큰을 대신하여 리프레시 토큰을 통해 새로운 토큰셋을 발급합니다.
     *
     * @param refreshToken 클라이언트가 전달한 리프레시 토큰
     * @return 새롭게 갱신된 토큰셋을 포함한 응답 DTO
     * @throws BaseException 토큰이 유효하지 않거나 만료된 경우 발생
     */
    @Transactional
    public AuthDto.LoginResponse refreshToken(String refreshToken) {
        // 1. 토큰 자체의 유효성 검사 (만료 여부, 서명 등)
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }

        // 2. Redis에서 해당 토큰 존재 여부 확인
        RefreshToken savedToken = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BaseException(ResultCode.REFRESH_TOKEN_NOT_FOUND));

        // 3. 토큰의 주인(Member)이 실제 존재하는지 확인
        Member member = memberRepository.findByIdOrThrow(savedToken.getMemberSeq());

        // 4. 기존 Redis 토큰 삭제 (새로운 토큰이 발급되므로 기존 토큰 파기)
        refreshTokenRepository.delete(savedToken);

        return buildAuthResponse(member, AuthStatus.SUCCESS);
    }

    /**
     * [로그아웃] Redis에 저장된 사용자의 리프레시 토큰을 삭제하여 로그아웃 처리합니다.
     *
     * @param memberSeq 로그아웃을 요청한 사용자의 PK
     */
    @Transactional
    public void logout(Long memberSeq) {
        refreshTokenRepository.deleteById(memberSeq);
    }

    /**
     * 비밀번호 변경 - 발급된 uuid를 사용합니다
     */
    @Transactional
    public void resetPassword(String email, String newPassword, String token) {
        String tokenKey = "email_token:PASSWORD_RESET:" + email;
        String savedToken = redisTemplate.opsForValue().getAndDelete(tokenKey);

        if (savedToken == null || !savedToken.equals(token)) {
            throw new BaseException(ResultCode.UNAUTHORIZED_EMAIL_VERIFICATION);
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

        member.updatePassword(passwordEncoder.encode(newPassword));
    }

    @Transactional(readOnly = true)
    public void validateEmailForPurpose(String email, EmailPurpose purpose) {
        if (purpose == EmailPurpose.SIGNUP) {
            // 회원가입 전: 이미 가입된 이메일이면 에러 뱉기
            memberService.checkEmailDuplicate(email);
        } else if (purpose == EmailPurpose.PASSWORD_RESET) {
            // 비밀번호 찾기 전: 우리 회원인지, 그리고 소셜 로그인 유저가 아닌지(비번이 있는지) 확인
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

            if (member.getPassword() == null) {
                throw new BaseException(ResultCode.OAUTH_MEMBER_CANNOT_RESET_PASSWORD);
            }
        }
    }

    // =========================================================================
    // 헬퍼 메서드 (Helper Methods)
    // =========================================================================


    /**
     * 공통 응답 생성 로직. 토큰셋(Access, Refresh)을 생성하고 AuthStatus를 포함한 LoginResponse를 빌드합니다.
     * 인증은 성공했더라도, 캐릭터가 없다면 무조건 온보딩 상태(REQUIRES_CHARACTER)로 강제 변환합니다.
     */
    private AuthDto.LoginResponse buildAuthResponse(Member member, AuthStatus status) {

        AuthStatus finalStatus = status;
        if (status == AuthStatus.SUCCESS && member.getBuddyCharacter() == null) {
            finalStatus = AuthStatus.REQUIRES_CHARACTER;
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getMemberSeq());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberSeq());

        return AuthDto.LoginResponse.builder()
                .status(finalStatus)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .member(MemberResponse.from(member))
                .build();
    }

}
