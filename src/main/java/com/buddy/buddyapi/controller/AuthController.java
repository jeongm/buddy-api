package com.buddy.buddyapi.controller;

import com.buddy.buddyapi.dto.request.OAuthLinkRequest;
import com.buddy.buddyapi.dto.request.TokenRefreshRequest;
import com.buddy.buddyapi.dto.response.LoginResponse;
import com.buddy.buddyapi.dto.request.MemberLoginRequest;
import com.buddy.buddyapi.dto.request.MemberRegisterRequest;
import com.buddy.buddyapi.dto.common.ApiResponse;
import com.buddy.buddyapi.dto.response.MemberSeqResponse;
import com.buddy.buddyapi.global.config.CustomUserDetails;
import com.buddy.buddyapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth", description = "Auth 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "일반 회원가입", description = "이메일, 비밀번호 등을 입력받아 회원가입을 진행합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberSeqResponse>> signup(
            @Valid @RequestBody MemberRegisterRequest request) {


        MemberSeqResponse result = authService.registerMember(request);
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

    @Operation(summary = "소셜 로그인 연동", description = "기존에 가입된 이메일일 경우 소셜 로그인 연동")
    @PostMapping("/oauth-link")
    public ResponseEntity<ApiResponse<LoginResponse>> linkSocialAccount(@RequestBody OAuthLinkRequest request) {
        LoginResponse result = authService.linkOauthAccount(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/delete-account")
    public ResponseEntity<ApiResponse<String>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails member
    ) {
        authService.deleteMember(member.memberSeq());
        return ResponseEntity.ok(ApiResponse.ok("탈퇴 완료",null));
    }



}
