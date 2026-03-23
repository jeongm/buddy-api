package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.domain.auth.component.OAuthUserInfo;
import com.buddy.buddyapi.domain.auth.dto.AuthDto;
import com.buddy.buddyapi.domain.auth.dto.OAuthDto;
import com.buddy.buddyapi.domain.auth.enums.AuthStatus;
import com.buddy.buddyapi.domain.auth.enums.EmailPurpose;
import com.buddy.buddyapi.domain.member.*;
import com.buddy.buddyapi.domain.member.dto.MemberResponse;
import com.buddy.buddyapi.domain.member.event.MemberWithdrawEvent;
import com.buddy.buddyapi.global.security.JwtTokenProvider;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberService memberService;
    private final OauthService oauthService;
    private final NotificationSettingService notificationSettingService;

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    /**
     * [일반(이메일) 회원가입 통합 로직]
     * 이메일 인증을 마친 사용자의 회원가입을 처리합니다.
     * 순수 계정(임시 닉네임, 캐릭터 null)만 생성한 뒤, 온보딩(캐릭터/닉네임 설정) 단계 진입을 위해
     * 기본(깡통) 알림 설정을 생성하고 자동 로그인(토큰 발급) 처리를 수행합니다.
     * (헬퍼 메서드를 통해 프론트엔드에 온보딩 필요 상태인 'REQUIRES_CHARACTER'를 반환합니다.)
     *
     * @param request 일반 회원가입 요청 DTO (이메일, 비밀번호, 인증 토큰)
     * @return 발급된 토큰셋(Access, Refresh)과 온보딩 필요 상태(REQUIRES_CHARACTER)를 포함한 응답 DTO
     * @throws BaseException 이메일 인증 토큰이 유효하지 않거나 만료된 경우, 또는 이미 가입된 이메일일 경우 발생
     */
    @Transactional
    public AuthDto.LoginResponse signup(AuthDto.SignUpRequest request) {
        // 이메일 인증 확인
        String tokenKey = "email_token:SIGNUP:" + request.email();
        String savedToken = redisTemplate.opsForValue().get(tokenKey);

        if (savedToken == null || !savedToken.equals(request.verificationToken())) {
            throw new BaseException(ResultCode.UNAUTHORIZED_EMAIL_VERIFICATION); // "이메일 인증이 만료되었거나 올바르지 않습니다."
        }

        memberService.checkEmailDuplicate(request.email());

        String encodedPassword = passwordEncoder.encode(request.password());

        Member newMember = memberService.registerLocalMember(request, encodedPassword);

        notificationSettingService.createDefaultSetting(newMember, false);

        redisTemplate.delete(tokenKey);
        // 회원 가입 완료 시 자동 로그인
        return issueTokensAndBuildResponse(newMember, AuthStatus.SUCCESS);
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
        Member member = memberService.getMemberByEmail(request.email());

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BaseException(ResultCode.INVALID_CREDENTIALS);
        }

        return issueTokensAndBuildResponse(member, AuthStatus.SUCCESS);
    }

    /**
     * [소셜 로그인 통합] 제공자(Google, Kakao, Naver, apple)의 토큰을 검증하고 서비스 로그인을 처리합니다.
     * apple의 경우 이메일 대신 oauthId(sub) 기준으로 처리한다.
     * 이미 가입된 이메일이 존재하지만 소셜 연동이 되어있지 않은 경우, 연동 대기 상태(REQUIRES_LINKING)를 반환합니다.
     *
     * @param request 제공자 이름(provider)과 인증 토큰(token)
     * @return 로그인 성공(SUCCESS) 또는 연동 필요(REQUIRES_LINKING) 상태가 포함된 응답 DTO
     */
    @Transactional
    public AuthDto.LoginResponse socialLogin(OAuthDto.LoginRequest request) throws JsonProcessingException {

        OAuthUserInfo userInfo = oauthService.verifyOauthToken(request.provider(), request.token());
        Provider provider = Provider.from(request.provider());

        // Apple은 oauthId(sub) 기준으로 조회 — 이메일은 두 번째 로그인부터 null이라 신뢰 불가
        if (provider == Provider.APPLE) {
            return handleAppleLogin(userInfo);
        }

        // 기존 회원 여부 확인
        Optional<Member> optionalMember = memberService.findMemberByEmail(userInfo.email());

        // [CASE] 이미 가입된 계정이 있는 경우
        if(optionalMember.isPresent()) {
            Member member = optionalMember.get();

            // [CASE] 연동이 필요한 경우 (REQUIRES_LINKING)
            if(!oauthService.isLinked(member, provider)) {
                return oauthService.handleLinkingRequired(request.provider(), userInfo);
            }

            // [CASE] 이미 연동됨 -> 로그인 성공 (SUCCESS)
            oauthService.updateSocialTokens(member,provider,
                    userInfo.socialAccessToken(), userInfo.socialRefreshToken());

            return issueTokensAndBuildResponse(member, AuthStatus.SUCCESS);
        }

        // [CASE] 아예 신규 유저 (가입 + 연동 + REQUIRES_CHARACTER)
        Member newMember = registerNewSocialMember(userInfo, provider);

        return issueTokensAndBuildResponse(newMember, AuthStatus.SUCCESS);

    }

    /**
     * [소셜 계정 연동] Redis에 임시 저장된 연동 정보를 확인하고, 기존 계정에 새로운 소셜 정보를 연동합니다.
     *
     * @param linkKey 연동 대기 상태에서 프론트엔드로 전달했던 임시 키 (linkKey)
     * @return 연동 완료 후 발급된 토큰셋을 포함한 로그인 성공 응답
     * @throws BaseException 키가 만료되었거나 조작된 경우 발생
     */
    @Transactional
    public AuthDto.LoginResponse linkOauthAccount(String linkKey) throws JsonProcessingException {

        OAuthLinkInfo linkInfo = oauthService.getAndRemoveLinkInfo(linkKey);

        Member member = memberService.getMemberByEmail(linkInfo.email());

        Provider provider = Provider.from(linkInfo.provider());

        if (oauthService.isLinked(member, provider)) {
            throw new BaseException(ResultCode.ALREADY_LINKED_ACCOUNT);
        }

        oauthService.saveOauthAccount(member, provider,
                linkInfo.oauthId(), linkInfo.socialAccessToken(), linkInfo.socialRefreshToken());

        return issueTokensAndBuildResponse(member, AuthStatus.SUCCESS);

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
        Member member = memberService.getMemberById(savedToken.getMemberId());

        // 4. 기존 Redis 토큰 삭제 (새로운 토큰이 발급되므로 기존 토큰 파기)
        refreshTokenRepository.delete(savedToken);

        return issueTokensAndBuildResponse(member, AuthStatus.SUCCESS);
    }

    /**
     * [로그아웃] Redis에 저장된 사용자의 리프레시 토큰을 삭제하여 로그아웃 처리합니다.
     *
     * @param memberId 로그아웃을 요청한 사용자의 PK
     */
    @Transactional
    public void logout(Long memberId) {
        refreshTokenRepository.deleteById(memberId);
    }

    /**
     * [비밀번호 재설정] 이메일 인증 토큰을 검증한 후 비밀번호를 재설정합니다.
     * 인증 토큰 검증 성공 시 Redis에서 해당 토큰을 즉시 삭제하여 재사용을 방지합니다.
     *
     * @param email       비밀번호를 재설정할 회원의 이메일
     * @param newPassword 새로 설정할 비밀번호 (평문)
     * @param token       이메일로 발급된 인증 토큰 (UUID)
     * @throws BaseException 인증 토큰이 만료되었거나 일치하지 않을 경우 발생
     */
    @Transactional
    public void resetPassword(String email, String newPassword, String token) {
        String tokenKey = "email_token:PASSWORD_RESET:" + email;
        String savedToken = redisTemplate.opsForValue().get(tokenKey);

        if (savedToken == null || !savedToken.equals(token)) {
            throw new BaseException(ResultCode.UNAUTHORIZED_EMAIL_VERIFICATION);
        }

        Member member = memberService.getMemberByEmail(email);

        member.updatePassword(passwordEncoder.encode(newPassword));

        redisTemplate.delete(tokenKey);

    }

    /**
     * 이메일 용도(회원가입/비밀번호 재설정)에 따라 이메일 유효성을 검증합니다.
     * - SIGNUP: 중복 이메일 여부 확인
     * - PASSWORD_RESET: 가입된 회원인지, 소셜 전용 계정이 아닌지 확인
     *
     * @param email   검증할 이메일 주소
     * @param purpose 이메일 사용 목적 (SIGNUP / PASSWORD_RESET)
     * @throws BaseException 목적에 따른 유효성 검증 실패 시 발생
     */
    public void validateEmailForPurpose(String email, EmailPurpose purpose) {
        if (purpose == EmailPurpose.SIGNUP) {
            // 회원가입 전: 이미 가입된 이메일이면 에러 뱉기
            memberService.checkEmailDuplicate(email);
        } else if (purpose == EmailPurpose.PASSWORD_RESET) {
            // 비밀번호 찾기 전: 우리 회원인지, 그리고 소셜 로그인 유저가 아닌지(비번이 있는지) 확인
            Member member = memberService.getMemberByEmail(email);

            if (member.getPassword() == null) {
                throw new BaseException(ResultCode.OAUTH_MEMBER_CANNOT_RESET_PASSWORD);
            }
        }
    }

    /**
     *  탈퇴할 회원의 Redis 리프레시 토큰을 파기합니다.
     * @param event 회원탈퇴 이벤트 수신
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberWithdraw(MemberWithdrawEvent event) {
        log.info("📢 [AuthService] 탈퇴 이벤트 수신! Redis 리프레시 토큰을 파기합니다. (memberId: {})", event.memberId());
        refreshTokenRepository.deleteById(event.memberId());
    }

    // =========================================================================
    // 헬퍼 메서드 (Helper Methods)
    // =========================================================================

    /**
     * [Apple 전용 로그인 처리]
     * Apple은 두 번째 로그인부터 이메일을 제공하지 않으므로 oauthId(sub) 기준으로 조회한다.
     *
     * @param userInfo AppleTokenVerifier에서 추출한 사용자 정보
     * @return 로그인 성공 응답 DTO
     */
    private AuthDto.LoginResponse handleAppleLogin(OAuthUserInfo userInfo) {
        // oauthId(sub) 기준으로 기존 연동 계정 조회
        Optional<OauthAccount> existingAccount = oauthService.findByProviderAndOauthId(
                Provider.APPLE, userInfo.oauthId());

        if (existingAccount.isPresent()) {
            // 기존 유저 — 바로 로그인
            return issueTokensAndBuildResponse(existingAccount.get().getMember(), AuthStatus.SUCCESS);
        }

        if (userInfo.email() == null) {
            throw new BaseException(ResultCode.INVALID_TOKEN, "Apple 최초 로그인 시 이메일이 제공되지 않았습니다.");
        }

        Member newMember = registerNewSocialMember(userInfo, Provider.APPLE);

        return issueTokensAndBuildResponse(newMember, AuthStatus.SUCCESS);
    }

    /**
     * 신규 소셜 회원 가입 공통 처리.
     * registerSocialMember, saveOauthAccount, createDefaultSetting은 항상 함께 실행되는
     * 하나의 행위이므로 하나의 메서드로 묶어 관리한다.
     * 가입 로직 변경(웰컴 푸시, 포인트 지급 등) 시 이 메서드만 수정하도록 함.
     *
     * @param userInfo 소셜 제공자에서 추출한 사용자 정보
     * @param provider 소셜 제공자 (GOOGLE, KAKAO, NAVER, APPLE)
     * @return 생성된 Member 엔티티
     */
    private Member registerNewSocialMember(OAuthUserInfo userInfo, Provider provider) {
        Member newMember = memberService.registerSocialMember(userInfo.email(), userInfo.name());
        oauthService.saveOauthAccount(newMember, provider,
                userInfo.oauthId(), userInfo.socialAccessToken(), userInfo.socialRefreshToken());
        notificationSettingService.createDefaultSetting(newMember, false);
        return newMember;
    }

    /**
     * 공통 토큰 발급 및 응답 생성.
     * 액세스/리프레시 토큰을 발급하고 Redis에 저장한다.
     * 캐릭터가 없는 경우 SUCCESS → REQUIRES_CHARACTER로 강제 변환하여
     * 프론트엔드가 온보딩 화면으로 이동할 수 있도록 한다.
     *
     * @param member 토큰을 발급할 대상 회원
     * @param status 현재 인증 상태 (SUCCESS, REQUIRES_LINKING 등)
     * @return 토큰셋과 AuthStatus를 포함한 LoginResponse
     */
    private AuthDto.LoginResponse issueTokensAndBuildResponse(Member member, AuthStatus status) {

        AuthStatus finalStatus = status;
        if (status == AuthStatus.SUCCESS && member.getBuddyCharacter() == null) {
            finalStatus = AuthStatus.REQUIRES_CHARACTER;
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getMemberId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());

        refreshTokenRepository.save(
                RefreshToken.builder()
                .memberId(member.getMemberId())
                .refreshToken(refreshToken)
                .build()
        );

        return AuthDto.LoginResponse.builder()
                .status(finalStatus)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .member(MemberResponse.from(member))
                .build();
    }

}
