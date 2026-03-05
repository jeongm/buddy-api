package com.buddy.buddyapi.domain.member;

import com.buddy.buddyapi.domain.member.dto.*;
import com.buddy.buddyapi.global.common.ApiResponse;
import com.buddy.buddyapi.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
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
    private final MemberInsightService insightService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails member) {
        return ResponseEntity.ok(ApiResponse.ok("내 정보 조회 성공", memberService.getUserDetails(member.memberSeq())));
    }

    @Operation(summary = "닉네임 수정", description = "사용자의 닉네임을 변경합니다.")
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<UpdateNicknameResponse>> updateNickname(
            @AuthenticationPrincipal CustomUserDetails member,
            @Valid @RequestBody UpdateNicknameRequest request
            ) {
        return ResponseEntity.ok(ApiResponse.ok("닉네임이 변경되었습니다",
                memberService.updateNickName(member.memberSeq(), request)));
    }

    @Operation(summary = "비밀번호 수정", description = "현재 비밀번호를 확인한 후 새로운 비밀번호로 변경합니다.")
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal CustomUserDetails member,
            @Valid @RequestBody UpdatePasswordRequest request) {
        memberService.updateMemberPassword(member.memberSeq(), request);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다.", null));
    }

    @Operation(summary = "내 캐릭터 변경", description = "현재 사용자의 버디 캐릭터를 변경합니다.")
    @PatchMapping("/me/character")
    public ResponseEntity<ApiResponse<String>> changeCharacter(
            @AuthenticationPrincipal CustomUserDetails member ,
            @Valid @RequestBody CharacterChangeRequest request) {
        memberService.changeMyCharacter(member.memberSeq(), request);
        return ResponseEntity.ok(ApiResponse.ok("캐릭터 변경 성공"));
    }

    @Operation(summary = "캐릭터 별명 변경", description = "캐릭터에게 지어준 별명을 수정합니다.")
    @PatchMapping("/me/character-name")
    public ResponseEntity<ApiResponse<String>> updateCharacterName(
            @AuthenticationPrincipal CustomUserDetails member,
            @Valid @RequestBody CharacterNameRequest request) {
        memberService.updateCharacterNickname(member.memberSeq(), request.characterName());
        return ResponseEntity.ok(ApiResponse.ok("캐릭터 이름이 변경되었습니다."));
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<String>> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails member
    ) {
        memberService.deleteMember(member.memberSeq());
        return ResponseEntity.ok(ApiResponse.ok("탈퇴 완료",null));
    }

    // TODO insight 기능 확장 시 url 변경 가능성 농후
    @Operation(
            summary = "주간 아이덴티티(칭호) 조회",
            description = "유저의 지난주 일기를 바탕으로 AI가 분석한 주간 칭호와 핵심 태그를 반환합니다. (이번 주 최초 조회 시에만 AI 분석이 실행되며, 이후에는 저장된 데이터를 빠르게 반환합니다. 작성된 일기가 없으면 null이 반환됩니다.)"
    )
    @GetMapping("/me/insight")
    public ResponseEntity<ApiResponse<WeeklyInsightResponse>> getMyWeeklyIdentity(
            @AuthenticationPrincipal CustomUserDetails member) {

        WeeklyInsightResponse response = insightService.getWeeklyInsight(member.memberSeq());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

}
