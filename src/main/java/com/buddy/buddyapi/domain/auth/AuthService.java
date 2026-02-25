package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.domain.auth.component.GoogleTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.KakaoTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.NaverTokenVerifier;
import com.buddy.buddyapi.domain.auth.component.OAuthUserInfo;
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
     * 이메일과 비밀번호를 기반으로 로그인을 처리하고 JWT 토큰을 발급합니다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @return 액세스 토큰, 리프레시 토큰 및 회원 정보를 포함한 응답 DTO
     * @throws BaseException 유저를 찾을 수 없거나 비밀번호가 일치하지 않을 경우 발생
     */
    @Transactional
    public LoginResponse localLoginMember(MemberLoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BaseException(ResultCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BaseException(ResultCode.INVALID_CREDENTIALS);
        }

        // 메서드 추출 적용
        return generateTokenSet(member);
    }

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

        // 5. 기존 Redis 토큰 삭제 (createRefreshToken에서 save를 하므로 여기서는 기존 것만 삭제)
        // 만약 RefreshToken 객체의 memberSeq가 @Id라면,
        // 새로운 save 시 덮어쓰기가 되므로 별도의 delete가 필요없을 수 있음
        refreshTokenRepository.delete(savedToken);

        return generateTokenSet(member);
    }

    /**
     * 공통 토큰 생성 및 응답 빌드 로직
     */
    private LoginResponse generateTokenSet(Member member) {
        Long memberSeq = member.getMemberSeq();

        // 1. 토큰 생성 (JwtTokenProvider 내부에서 RefreshToken Redis 저장까지 처리됨)
        String accessToken = jwtTokenProvider.createAccessToken(memberSeq);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberSeq);

        // 2. 응답 생성
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .member(MemberResponse.from(member))
                .build();
    }

    @Transactional
    public LoginResponse socialTokenLogin(OAuthDto.LoginRequest request) {
        // 1. Provider(구글/카카오/네이버)에 맞는 검증기를 통해 유저 정보 추출
        OAuthUserInfo userInfo = verifyOauthToken(request.provider(), request.token());

        Member member = memberRepository.findByEmail(userInfo.email())
                .orElseGet(() -> {
                   Member newMember = Member.builder()
                           .email(userInfo.email())
                           .nickname(userInfo.name())
                           .build();
                   return memberRepository.save(newMember);
                });

        Provider provider = Provider.from(request.provider());
        if(!oauthAccountRepository.existsByMemberAndProvider(member,provider)){
            OauthAccount oauthAccount = OauthAccount.builder()
                    .provider(provider)
                    .oauthId(userInfo.oauthId())
                    .member(member)
                    .build();
            oauthAccountRepository.save(oauthAccount);
        }

        return generateTokenSet(member);
    }

    private OAuthUserInfo verifyOauthToken(String provider, String token) {
        return switch (provider.toLowerCase()) {
            case "google" -> googleTokenVerifier.verify(token);
            case "kakao" -> kakaoTokenVerifier.verify(token);
            case "naver" -> naverTokenVerifier.verify(token);
            default -> throw new BaseException(ResultCode.UNSUPPORTED_PROVIDER); // 지원하지 않는 소셜 로그인
        };
    }

    // 로그인 성공 시 토큰 교환
    @Transactional
    public LoginResponse oauthLoginSuccess(String key) {
        String redisKey = "OAUTH_SUCCESS:" + key;
        String redisValue = redisTemplate.opsForValue().getAndDelete(redisKey);

        if (redisValue == null) {
            throw new BaseException(ResultCode.INVALID_TOKEN);
        }

        String[] parts = redisValue.split(":");

        Long memberSeq = Long.parseLong(parts[0]);

        Member member = memberRepository.findByIdOrThrow(memberSeq);

        return generateTokenSet(member);
    }

    @Transactional
    public LoginResponse linkOauthAccount(String key) throws JsonProcessingException {

        // Redis에서 검증된 진짜 정보 꺼내기
        String redisKey = "OAUTH_LINK:" + key;
        String jsonValue = redisTemplate.opsForValue().getAndDelete(redisKey);

        if (jsonValue == null) {
            throw new BaseException(ResultCode.INVALID_TOKEN); // 만료되거나 조작된 요청
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

        return generateTokenSet(member);

    }

    @Transactional
    public void logout(Long memberSeq) {
        refreshTokenRepository.deleteById(memberSeq);
    }

}
