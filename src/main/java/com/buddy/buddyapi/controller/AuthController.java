package com.buddy.buddyapi.controller;

import com.buddy.buddyapi.dto.request.TokenRefreshRequest;
import com.buddy.buddyapi.dto.response.LoginResponse;
import com.buddy.buddyapi.dto.request.MemberLoginRequest;
import com.buddy.buddyapi.dto.request.MemberRegisterRequest;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.dto.common.ApiResponse;
import com.buddy.buddyapi.service.AuthService;
import com.buddy.buddyapi.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Auth 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "일반 회원가입", description = "이메일, 비밀번호 등을 입력받아 회원가입을 진행합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberResponse>> signup(
            @Valid @RequestBody MemberRegisterRequest request) {


        MemberResponse result = authService.registerMember(request);
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
            @Valid@RequestBody TokenRefreshRequest request) {
        LoginResponse newAuthToken = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("토큰 재발급 성공", newAuthToken));
    }


}
