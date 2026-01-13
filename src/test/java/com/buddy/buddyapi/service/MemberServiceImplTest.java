package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.response.LoginResponse;
import com.buddy.buddyapi.dto.request.MemberLoginRequest;
import com.buddy.buddyapi.dto.request.MemberRegisterRequest;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.entity.BuddyCharacter;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.global.config.JwtTokenProvider;
import com.buddy.buddyapi.repository.BuddyCharacterRepository;
import com.buddy.buddyapi.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BuddyCharacterRepository characterRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthService authService;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원가입_성공")
    void registerMember() {
        // 1. 준비 (given)
        MemberRegisterRequest request = new MemberRegisterRequest("test@test.com", "password", "nickname", 1L);
        BuddyCharacter character = new BuddyCharacter("buddy", "기본버디", "설명","/");

        // 중요: save가 호출될 때 반환할 가짜 멤버 객체 생성 (ID값이 들어있어야 함)
        Member member = Member.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .buddyCharacter(character)
                .build();

        // 리플렉션으로 가짜 ID 주입!
        ReflectionTestUtils.setField(member, "memberSeq", 1L);

        // Mock 객체 행동 정의
        given(memberRepository.existsByEmail(anyString())).willReturn(false);
        given(characterRepository.findById(anyLong())).willReturn(Optional.of(character));
        given(passwordEncoder.encode(anyString())).willReturn("encoded_password");
        given(memberRepository.save(any(Member.class))).willReturn(member);

        // 2. 실행 (when)
        MemberResponse response = authService.registerMember(request);

        // 3. 검증 (then)
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.memberSeq()).isEqualTo(1L);
    }

    @Test
    @DisplayName("로그인_성공_테스트")
    void localLoginMember() {
// given
        String email = "test@test.com";
        String password = "password123!";
        String encodedPassword = "encoded_password";

        Member member = Member.builder()
                .email(email)
                .password(encodedPassword)
                .build();
        ReflectionTestUtils.setField(member, "memberSeq", 1L);

        given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
        // 평문 비번과 암호화 비번이 일치한다고 가정
        given(passwordEncoder.matches(password, encodedPassword)).willReturn(true);
        // 가짜 토큰 반환 설정
        given(jwtTokenProvider.createAccessToken(member.getMemberSeq())).willReturn("fake-access-token");
        given(jwtTokenProvider.createRefreshToken(member.getMemberSeq())).willReturn("fake-refresh-token");

        // when
        LoginResponse response = authService.localLoginMember(new MemberLoginRequest(email, password));

        // then
        assertThat(response.accessToken()).isEqualTo("fake-access-token");
        assertThat(response.member().email()).isEqualTo(email);
    }

    @Test
    void updateNickName() {
    }

    @Test
    void updateMemberPassword() {
    }

    @Test
    void getUserDetails() {
    }

    @Test
    void loadUserByUsername() {
    }
}