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
        LoginResponse result = authService.localLoginMember(request);
        return ResponseEntity.ok(ApiResponse.ok("로그인 성공", result));
    }

    @Operation(summary = "소셜 로그인 성공 정보 조회", description = "발급받은 임시 키로 토큰 정보를 교환합니다.")
    @GetMapping("/oauth/success")
    public ResponseEntity<ApiResponse<LoginResponse>> oauthLoginSuccess(@RequestParam String key) {
        LoginResponse result = authService.oauthLoginSuccess(key);
        return ResponseEntity.ok(ApiResponse.ok("소셜 로그인 성공", result));
    }


    @Operation(summary = "소셜 로그인 연동", description = "발급받은 키를 이용해 소셜 계정을 연동")
    @PostMapping("/oauth/link")
    public ResponseEntity<ApiResponse<LoginResponse>> linkSocialAccount(@RequestBody AuthDto.OAuthLinkRequest request) throws JsonProcessingException {
        LoginResponse result = authService.linkOauthAccount(request.key());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "토큰 재발급", description = "토큰 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        LoginResponse newAuthToken = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("토큰 재발급 성공", newAuthToken));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails member) {
        authService.logout(member.memberSeq());

        return ResponseEntity.ok(ApiResponse.ok("로그아웃 성공", null));
    }


    @Operation(summary = "회원가입 인증 이메일 발송", description = "입력한 이메일로 6자리 인증 코드를 보냅니다.")
    @PostMapping("/signup/email")
    public ResponseEntity<ApiResponse<Void>> requestEmail(
            @RequestBody EmailRequest request
    ) {
        memberService.checkEmailDuplicate(request.email());
        emailService.checkSendRateLimit(request.email());
        emailService.sendVerificationCode(request.email());
        return ResponseEntity.ok(ApiResponse.ok("인증 코드가 발송되었습니다.", null));

    }

    @Operation(summary = "인증 코드 검증", description = "사용자가 입력한 코드가 유효한지 확인합니다.")
    @PostMapping("/signup/email/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyEmail(
            @RequestBody EmailVerifyRequest request
            ) {
        boolean isVerified = emailService.verifyCode(request.email(), request.code());

        if (isVerified) {
            return ResponseEntity.ok(ApiResponse.ok("인증에 성공했습니다.", true));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(ResultCode.INVALID_INPUT, "인증 코드가 틀렸거나 만료되었습니다."));
        }
    }
}
