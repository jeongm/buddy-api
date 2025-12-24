package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.response.CharacterResponse;
import com.buddy.buddyapi.dto.response.MemberResponse;
import com.buddy.buddyapi.entity.BuddyCharacter;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.repository.BuddyCharacterRepository;
import com.buddy.buddyapi.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuddyCharacterService {

    private final BuddyCharacterRepository characterRepository;
    private final MemberRepository memberRepository;

    // 1. 캐릭터 목록 전체 조회
    public List<CharacterResponse> getAllCharacters() {
        return characterRepository.findAll().stream()
                .map(CharacterResponse::from)
                .toList();
    }

    // 2. 내 캐릭터 변경
    @Transactional
    public MemberResponse changeMyCharacter(Member member, Long characterSeq) {
        BuddyCharacter newCharacter = characterRepository.findById(characterSeq)
                .orElseThrow(() -> new BaseException(ResultCode.CHARACTER_NOT_FOUND));

        member.changeCharacter(newCharacter);
        memberRepository.save(member);

        return MemberResponse.from(member);
    }

    // 3. 캐릭터 별명 변경
    @Transactional
    public void updateCharacterNickname(Member member, String newName) {
        member.updateCharacterNickname(newName);
        memberRepository.save(member);
    }
}
