package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.domain.auth.dto.*;
import com.buddy.buddyapi.domain.member.MemberService;
import com.buddy.buddyapi.domain.member.dto.MemberRegisterRequest;
import com.buddy.buddyapi.domain.member.dto.MemberSeqResponse;
import com.buddy.buddyapi.global.common.ApiResponse;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.global.security.CustomUserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Auth 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final MemberService memberService;

    // =========================================================================
    // 회원가입 및 일반 로그인
    // =========================================================================
    @Operation(summary = "일반 회원가입", description = "이메일, 비밀번호 등을 입력받아 회원가입을 진행합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberSeqResponse>> signup(
            @Valid @RequestBody MemberRegisterRequest request) {

        MemberSeqResponse result = memberService.registerMember(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("회원가입 완료", result));
    }


    @Operation(summary = "일반 로그인", description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody MemberLoginRequest request) {
        LoginResponse result = authService.localLogin(request);
        return ResponseEntity.ok(ApiResponse.ok("로그인 성공", result));
    }

    // =========================================================================
    // 소셜 로그인 및 연동
    // =========================================================================
    @Operation(summary = "소셜 로그인", description = "제공자(구글, 카카오, 네이버)의 토큰을 통해 소셜 로그인을 진행합니다. 연동이 필요한 경우 REQUIRES_LINKING 상태를 반환합니다.")
    @PostMapping("/login/social")
    public ResponseEntity<ApiResponse<LoginResponse>> socialLogin(
            @Valid @RequestBody OAuthDto.LoginRequest request
    ) throws JsonProcessingException {
        LoginResponse result = authService.socialLogin(request);

        return ResponseEntity.ok(ApiResponse.ok("소셜 로그인 처리 완료", result));
    }


    @Operation(summary = "소셜 계정 연동 완료", description = "소셜 로그인 시 연동이 필요했던 계정에 대해, 발급받은 linkKey를 전달하여 연동을 완료하고 토큰을 발급받습니다.")    @PostMapping("/social/link")
    public ResponseEntity<ApiResponse<LoginResponse>> linkSocialAccount(
            @RequestBody OAuthDto.OAuthLinkRequest request) throws JsonProcessingException {
        LoginResponse result = authService.linkOauthAccount(request.key());
        return ResponseEntity.ok(ApiResponse.ok("소셜 계정 연동 및 로그인 성공", result));
    }

    // =========================================================================
    // 토큰 관리 및 로그아웃
    // =========================================================================
    @Operation(summary = "토큰 재발급", description = "만료된 액세스 토큰을 대신하여 리프레시 토큰을 이용해 새로운 토큰셋을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        LoginResponse newAuthToken = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("토큰 재발급 성공", newAuthToken));
    }

    @Operation(summary = "로그아웃", description = "서버(Redis)에 저장된 사용자의 리프레시 토큰을 삭제하여 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails member) {
        authService.logout(member.memberSeq());

        return ResponseEntity.ok(ApiResponse.ok("로그아웃 성공", null));
    }

    // =========================================================================
    // 이메일 인증 플로우
    // =========================================================================
    @Operation(summary = "회원가입 인증 이메일 발송", description = "중복 가입 여부 확인 후, 입력한 이메일로 6자리 인증 코드를 발송합니다.")
    @PostMapping("/signup/email")
    public ResponseEntity<ApiResponse<Void>> requestEmail(
            @Valid @RequestBody EmailRequest request
    ) {
        memberService.checkEmailDuplicate(request.email());
        emailService.checkSendRateLimit(request.email());
        emailService.sendVerificationCode(request.email());
        return ResponseEntity.ok(ApiResponse.ok("인증 코드가 발송되었습니다.", null));

    }

    @Operation(summary = "인증 코드 검증", description = "사용자가 입력한 코드가 유효한지 확인합니다.")
    @PostMapping("/signup/email/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyEmail(
            @Valid @RequestBody EmailVerifyRequest request
            ) {
        boolean isVerified = emailService.verifyCode(request.email(), request.code());

        return ResponseEntity.ok(ApiResponse.ok("이메일 인증 성공", isVerified));

    }
}
