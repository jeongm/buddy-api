package com.buddy.buddyapi.controller;

import com.buddy.buddyapi.dto.common.ApiResponse;
import com.buddy.buddyapi.dto.request.CharacterNameRequest;
import com.buddy.buddyapi.dto.response.CharacterResponse;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.service.BuddyCharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Character", description = "캐릭터 관련 API")
public class CharacterController {
    private final BuddyCharacterService characterService;

    @Operation(summary = "캐릭터 목록 조회", description = "선택 가능한 모든 캐릭터를 조회합니다.")
    @GetMapping("/characters")
    public ApiResponse<List<CharacterResponse>> getCharacters() {
        return ApiResponse.success(characterService.getAllCharacters());
    }

    @Operation(summary = "내 캐릭터 변경", description = "현재 사용자의 버디 캐릭터를 변경합니다.")
    @PatchMapping("/users/me/character")
    public ApiResponse<MemberResponse> changeCharacter(
            @AuthenticationPrincipal Member member,
            @RequestBody Map<String, Long> request) {
        Long characterSeq = request.get("characterSeq");
        return ApiResponse.success(characterService.changeMyCharacter(member, characterSeq));
    }

    @Operation(summary = "캐릭터 별명 변경", description = "캐릭터에게 지어준 별명을 수정합니다.")
    @PatchMapping("/users/me/character-name")
    public ApiResponse<String> updateCharacterName(
            @AuthenticationPrincipal Member member,
            @RequestBody CharacterNameRequest request) {
        characterService.updateCharacterNickname(member, request.characterName());
        return ApiResponse.success("캐릭터 이름이 변경되었습니다.");
    }
}
