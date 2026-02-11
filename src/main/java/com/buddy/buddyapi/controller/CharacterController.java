package com.buddy.buddyapi.controller;

import com.buddy.buddyapi.dto.common.ApiResponse;
import com.buddy.buddyapi.dto.request.CharacterChangeRequest;
import com.buddy.buddyapi.dto.request.CharacterNameRequest;
import com.buddy.buddyapi.dto.response.CharacterResponse;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.global.config.CustomUserDetails;
import com.buddy.buddyapi.service.BuddyCharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Character", description = "캐릭터 관련 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CharacterController {
    private final BuddyCharacterService characterService;

    @Operation(summary = "캐릭터 목록 조회", description = "선택 가능한 모든 캐릭터를 조회합니다.")
    @GetMapping("/characters")
    public ResponseEntity<ApiResponse<List<CharacterResponse>>> getCharacters() {
        return ResponseEntity.ok(ApiResponse.ok(characterService.getAllCharacters()));
    }

    @Operation(summary = "내 캐릭터 변경", description = "현재 사용자의 버디 캐릭터를 변경합니다.")
    @PatchMapping("/users/me/character")
    public ResponseEntity<ApiResponse<MemberResponse>> changeCharacter(
            @AuthenticationPrincipal CustomUserDetails member ,
            @Valid @RequestBody CharacterChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(characterService.changeMyCharacter(member.memberSeq(), request)));
    }

    @Operation(summary = "캐릭터 별명 변경", description = "캐릭터에게 지어준 별명을 수정합니다.")
    @PatchMapping("/users/me/character-name")
    public ResponseEntity<ApiResponse<String>> updateCharacterName(
            @AuthenticationPrincipal CustomUserDetails member,
            @Valid @RequestBody CharacterNameRequest request) {
        characterService.updateCharacterNickname(member.memberSeq(), request.characterName());
        return ResponseEntity.ok(ApiResponse.ok("캐릭터 이름이 변경되었습니다."));
    }
}
