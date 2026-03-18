package com.buddy.buddyapi.domain.character;

import com.buddy.buddyapi.domain.character.dto.CharacterResponse;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuddyCharacterService {

    private final BuddyCharacterRepository characterRepository;

    /**
     * 시스템에 등록된 모든 버디 캐릭터 목록을 조회합니다.
     *
     * @return 캐릭터 정보(ID, 이름, 설명 등) 리스트
     */
    public List<CharacterResponse> getAllCharacters() {
        return characterRepository.findAll().stream()
                .map(CharacterResponse::from)
                .toList();
    }

    public BuddyCharacter getCharacter(Long characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new BaseException(ResultCode.CHARACTER_NOT_FOUND));
    }

    // 가짜 프록시 객체만 필요할 때 (캐릭터 변경 용 - SELECT 쿼리 X)
    public BuddyCharacter getCharacterProxy(Long characterId) {
        return characterRepository.getReferenceById(characterId);
    }



}
