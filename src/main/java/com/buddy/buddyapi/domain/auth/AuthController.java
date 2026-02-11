package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.domain.auth.dto.OAuthLinkRequest;
import com.buddy.buddyapi.domain.auth.dto.TokenRefreshRequest;
import com.buddy.buddyapi.domain.auth.dto.LoginResponse;
import com.buddy.buddyapi.domain.auth.dto.MemberLoginRequest;
import com.buddy.buddyapi.global.common.ApiResponse;
import com.buddy.buddyapi.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Auth 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;



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





}
