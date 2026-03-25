package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.domain.member.dto.*;
import com.buddy.buddyapi.global.common.ApiResponse;
import com.buddy.buddyapi.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "맴버 API")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails member) {
        return ResponseEntity.ok(ApiResponse.ok("내 정보 조회 성공", memberService.getUserDetails(member.memberId())));
    }

    @Operation(summary = "회원가입 직후 온보딩", description = "초기 닉네임, 캐릭터, 알림 설정을 한 번에 완료합니다.")
    @PatchMapping("/me/onboarding")
    public ResponseEntity<ApiResponse<Void>> completeOnboarding(
            @AuthenticationPrincipal CustomUserDetails member,
            @Valid @RequestBody CompleteOnboardingRequest request) {

        memberService.completeOnboarding(member.memberId(), request);

        return ResponseEntity.ok(ApiResponse.ok("온보딩이 완료되었습니다.", null));
    }

    @Operation(summary = "닉네임 수정", description = "사용자의 닉네임을 변경합니다.")
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<UpdateNicknameResponse>> updateNickname(
            @AuthenticationPrincipal CustomUserDetails member,
            @Valid @RequestBody UpdateNicknameRequest request
            ) {
        return ResponseEntity.ok(ApiResponse.ok("닉네임이 변경되었습니다",
                memberService.updateNickName(member.memberId(), request)));
    }

    @Operation(summary = "현재 비밀번호 확인", description = "비밀번호 변경 전, 현재 비밀번호가 맞는지 1차로 검증합니다.")
    @PostMapping("/me/password/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCurrentPassword(
            @AuthenticationPrincipal CustomUserDetails member,
            @Valid @RequestBody VerifyPasswordRequest request) {

        memberService.verifyPassword(member.memberId(), request.currentPassword());

        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 확인되었습니다.", null));
    }

    @Operation(summary = "비밀번호 수정", description = "현재 비밀번호를 확인한 후 새로운 비밀번호로 변경합니다.")
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal CustomUserDetails member,
            @Valid @RequestBody UpdatePasswordRequest request) {
        memberService.updateMemberPassword(member.memberId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다.", null));
    }

    @Operation(summary = "내 캐릭터 변경", description = "현재 사용자의 버디 캐릭터를 변경합니다.")
    @PatchMapping("/me/character")
    public ResponseEntity<ApiResponse<String>> changeCharacter(
            @AuthenticationPrincipal CustomUserDetails member ,
            @Valid @RequestBody ChangeCharacterRequest request) {
        memberService.changeMyCharacter(member.memberId(), request);
        return ResponseEntity.ok(ApiResponse.ok("캐릭터 변경 성공"));
    }

    @Operation(summary = "캐릭터 별명 변경", description = "캐릭터에게 지어준 별명을 수정합니다.")
    @PatchMapping("/me/character-name")
    public ResponseEntity<ApiResponse<String>> updateCharacterName(
            @AuthenticationPrincipal CustomUserDetails member,
            @Valid @RequestBody UpdateCharacterNameRequest request) {
        memberService.updateCharacterNickname(member.memberId(), request.characterName());
        return ResponseEntity.ok(ApiResponse.ok("캐릭터 이름이 변경되었습니다."));
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<String>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails member
    ) {
        memberService.deleteMember(member.memberId());
        return ResponseEntity.ok(ApiResponse.ok("탈퇴 완료",null));
    }

    @Operation(summary = "FCM 토큰 갱신/저장", description = "로그인 시 발급받은 디바이스의 FCM 토큰을 서버에 저장합니다. (알림 발송용)")
    @PatchMapping("/push-token")
    public ResponseEntity<ApiResponse<Void>> updatePushToken(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails member,
            @RequestBody UpdatePushTokenRequest request) {

        memberService.updatePushToken(member.memberId(), request.pushToken());

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

}
