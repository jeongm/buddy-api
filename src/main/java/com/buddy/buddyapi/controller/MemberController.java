package com.buddy.buddyapi.controller;

import com.buddy.buddyapi.dto.common.ApiResponse;
import com.buddy.buddyapi.dto.request.UpdateNicknameRequest;
import com.buddy.buddyapi.dto.request.UpdatePasswordRequest;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.dto.response.UpdateNicknameResponse;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyInfo(@AuthenticationPrincipal Member member) {
        return ApiResponse.success("내 정보 조회 성공", memberService.getUserDetails(member.getMemberSeq()));
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<UpdateNicknameResponse> updateNickname(
            @AuthenticationPrincipal Member member,
            @RequestBody UpdateNicknameRequest request
            ) {
        return ApiResponse.success("닉네임이 변경되었습니다",
                memberService.updateNickName(member.getMemberSeq(), request));
    }

    @PatchMapping("/me/password")
    public ApiResponse<Void> updatePassword(
            @AuthenticationPrincipal Member member,
            @RequestBody UpdatePasswordRequest request) {
        memberService.updateMemberPassword(member.getMemberSeq(), request);
        return ApiResponse.success("비밀번호가 변경되었습니다.", null);
    }

}
