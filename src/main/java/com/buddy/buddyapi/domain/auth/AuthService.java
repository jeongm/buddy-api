package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.domain.auth.component.GoogleTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.KakaoTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.NaverTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.OAuthUserInfo;
import com.buddy.buddyapi.domain.auth.dto.AuthStatus;
import com.buddy.buddyapi.domain.auth.dto.OAuthDto;
import com.buddy.buddyapi.domain.member.*;
import com.buddy.buddyapi.domain.auth.dto.MemberLoginRequest;
import com.buddy.buddyapi.domain.auth.dto.LoginResponse;
import com.buddy.buddyapi.domain.member.dto.MemberResponse;
import com.buddy.buddyapi.global.security.JwtTokenProvider;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final OauthAccountRepository oauthAccountRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private final GoogleTokenVerifier googleTokenVerifier;
    private final KakaoTokenVerifier kakaoTokenVerifier;
    private final NaverTokenVerifier naverTokenVerifier;

    /**
     * [일반 로그인] 이메일과 비밀번호를 기반으로 로그인을 처리합니다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @return 성공 상태(SUCCESS), 액세스 토큰, 리프레시 토큰, 회원 정보를 포함한 응답 DTO
     * @throws BaseException 유저를 찾을 수 없거나 비밀번호가 일치하지 않을 경우 발생
     */
    @Transactional
    public LoginResponse localLogin(MemberLoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
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
    public LoginResponse refreshToken(String refreshToken) {
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
     * [소셜 로그인 통합] 제공자(Google, Kakao, Naver)의 토큰을 검증하고 서비스 로그인을 처리합니다.
     * 이미 가입된 이메일이 존재하지만 소셜 연동이 되어있지 않은 경우, 연동 대기 상태(REQUIRES_LINKING)를 반환합니다.
     *
     * @param request 제공자 이름(provider)과 인증 토큰(token)
     * @return 로그인 성공(SUCCESS) 또는 연동 필요(REQUIRES_LINKING) 상태가 포함된 응답 DTO
     */
    @Transactional
    public LoginResponse socialLogin(OAuthDto.LoginRequest request) throws JsonProcessingException {
        // 1. 검증 및 정보 추출 (verifyOauthToken)
        OAuthUserInfo userInfo = verifyOauthToken(request.provider(), request.token());
        Provider provider = Provider.from(request.provider());

        // 기존 회원 여부 확인
        Optional<Member> optionalMember = memberRepository.findByEmail(userInfo.email());

        // [CASE] 이미 가입된 계정이 있는 경우
        if(optionalMember.isPresent()) {
            Member member = optionalMember.get();

            // [CASE] 연동이 필요한 경우 (REQUIRES_LINKING)
            if(!oauthAccountRepository.existsByMemberAndProvider(member,provider)) {
                return handleLinkingRequired(request, userInfo);
            }

            // [CASE] 이미 연동됨 -> 로그인 성공 (SUCCESS)
            return buildAuthResponse(member, AuthStatus.SUCCESS);
        }

        // [CASE] 아예 신규 유저 (가입 + 연동 + SUCCESS)
        Member newMember = registerNewSocialMember(userInfo,provider);
        return buildAuthResponse(newMember, AuthStatus.SUCCESS);

    }

    /**
     * [소셜 계정 연동 완료] Redis에 임시 저장된 연동 정보를 확인하고, 기존 계정에 소셜 정보를 귀속시킵니다.
     *
     * @param key 연동 대기 상태에서 프론트엔드로 전달했던 임시 키 (linkKey)
     * @return 연동 완료 후 발급된 토큰셋을 포함한 로그인 성공 응답
     * @throws BaseException 키가 만료되었거나 조작된 경우 발생
     */
    @Transactional
    public LoginResponse linkOauthAccount(String key) throws JsonProcessingException {

        // Redis에서 검증된 진짜 정보 꺼내기
        String redisKey = "OAUTH_LINK:" + key;
        String jsonValue = redisTemplate.opsForValue().getAndDelete(redisKey);

        if (jsonValue == null) {
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }

        OAuthDto.OauthLinkInfo linkInfo = objectMapper.readValue(jsonValue, OAuthDto.OauthLinkInfo.class);

        Member member = memberRepository.findByEmail(linkInfo.email())
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

        Provider provider = Provider.from(linkInfo.provider());
        if (oauthAccountRepository.existsByMemberAndProvider(member, provider)) {
            throw new BaseException(ResultCode.ALREADY_LINKED_ACCOUNT);
        }

        OauthAccount oauthAccount = OauthAccount.builder()
                .provider(provider)
                .oauthId(linkInfo.oauthId())
                .member(member)
                .build();

        oauthAccountRepository.save(oauthAccount);

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

    // =========================================================================
    // 헬퍼 메서드 (Helper Methods)
    // =========================================================================

    /**
     * 각 소셜 제공자별 알맞은 토큰 검증기(Verifier)를 호출하여 유저 정보를 추출합니다.
     */
    private OAuthUserInfo verifyOauthToken(String provider, String token) {
        return switch (provider.toLowerCase()) {
            case "google" -> googleTokenVerifier.verify(token);
            case "kakao" -> kakaoTokenVerifier.verify(token);
            case "naver" -> naverTokenVerifier.verify(token);
            default -> throw new BaseException(ResultCode.UNSUPPORTED_PROVIDER);
        };
    }

    /**
     * 소셜 연동이 필요한 유저의 정보를 Redis에 10분간 임시 보관하고, 프론트엔드에 REQUIRES_LINKING 상태를 반환합니다.
     */
    private LoginResponse handleLinkingRequired(OAuthDto.LoginRequest request, OAuthUserInfo userInfo) throws JsonProcessingException {
        String linkKey = UUID.randomUUID().toString();
        OAuthDto.OauthLinkInfo linkInfo = OAuthDto.OauthLinkInfo.builder()
                .email(userInfo.email())
                .provider(request.provider())
                .oauthId(userInfo.oauthId())
                .build();

        redisTemplate.opsForValue().set("OAUTH_LINK:" + linkKey,
                objectMapper.writeValueAsString(linkInfo), Duration.ofMinutes(10));

        return LoginResponse.builder()
                .status(AuthStatus.REQUIRES_LINKING)
                .linkKey(linkKey)
                .build();
    }

    /**
     * 신규 소셜 로그인 유저를 데이터베이스에 등록(회원가입)하고 연동 정보를 저장합니다.
     */
    private Member registerNewSocialMember(OAuthUserInfo userInfo, Provider provider) {
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
     * 공통 응답 생성 로직. 토큰셋(Access, Refresh)을 생성하고 AuthStatus를 포함한 LoginResponse를 빌드합니다.
     */
    private LoginResponse buildAuthResponse(Member member, AuthStatus status) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getMemberSeq());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberSeq());

        return LoginResponse.builder()
                .status(status)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .member(MemberResponse.from(member))
                .build();
    }



}
