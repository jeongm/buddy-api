package com.buddy.buddyapi.controller;

import com.buddy.buddyapi.dto.response.LoginResponse;
import com.buddy.buddyapi.dto.request.MemberLoginRequest;
import com.buddy.buddyapi.dto.request.MemberRegisterRequest;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.dto.common.ApiResponse;
import com.buddy.buddyapi.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberResponse>> signup(@RequestBody MemberRegisterRequest request) {
        MemberResponse result = memberService.registerMember(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입 완료", result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody MemberLoginRequest request) {
        // TODO: 여기서 JWT 토큰을 생성하는 로직이 필요합니다.
        LoginResponse result = memberService.localLoginMember(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", result));
    }

}
