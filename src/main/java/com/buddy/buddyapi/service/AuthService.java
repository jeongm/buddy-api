package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.request.MemberLoginRequest;
import com.buddy.buddyapi.dto.request.MemberRegisterRequest;
import com.buddy.buddyapi.dto.response.LoginResponse;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.dto.response.MemberSeqResponse;
import com.buddy.buddyapi.entity.BuddyCharacter;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.entity.RefreshToken;
import com.buddy.buddyapi.global.config.JwtTokenProvider;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.repository.BuddyCharacterRepository;
import com.buddy.buddyapi.repository.MemberRepository;
import com.buddy.buddyapi.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
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
    private final BuddyCharacterRepository characterRepository;

    /**
     * 일반 회원가입 처리
     * @param request 회원가입 정보 (이메일, 비밀번호, 닉네임, 캐릭터 번호 등)
     * @return memberResponse 가입 완료된 회원의 정보 DTO
     * @throws BaseException 이미 존재하는 이메일이거나 캐릭터가 없을 경우 발생
     */
    @Transactional
    public MemberSeqResponse registerMember(MemberRegisterRequest request) {

        if(memberRepository.existsByEmail(request.getEmail())) {
            throw new BaseException(ResultCode.EMAIL_DUPLICATED);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 초기 캐릭터 조회 및 유효성 검사
        // TODO DB에 1번 캐릭터가 반드시 존재해야 한다는 강력한 전제가 필요 -
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

        if(!passwordEncoder.matches(request.getPassword(), member.getPassword())){
            throw new BaseException(ResultCode.INVALID_CREDENTIALS);
        }

        // 메서드 추출 적용
        return generateTokenSet(member);
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        // 1. 토큰 자체의 유효성 검사 (만료 여부, 서명 등)
        if(!jwtTokenProvider.validateToken(refreshToken)) {
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
    public void logout(Long memberSeq) {
        refreshTokenRepository.deleteById(memberSeq);
    }

}
