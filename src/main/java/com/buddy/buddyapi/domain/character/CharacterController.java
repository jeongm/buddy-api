package com.buddy.buddyapi.domain.character;

import com.buddy.buddyapi.global.common.ApiResponse;
import com.buddy.buddyapi.domain.character.dto.CharacterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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


}
