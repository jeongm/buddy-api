package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.request.CharacterChangeRequest;
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

    /**
     * 회원이 사용할 버디 캐릭터를 변경합니다.
     *
     * @param memberSeq 현재 로그인한 회원 정보
     * @param request    변경하고자 하는 캐릭터의 식별자가 담긴 DTO
     * @return 캐릭터가 변경된 후의 회원 정보 응답 DTO
     * @throws BaseException 존재하지 않는 캐릭터 ID이거나 회원을 찾을 수 없을 경우 발생
     */
    @Transactional
    public MemberResponse changeMyCharacter(Long memberSeq, CharacterChangeRequest request) {
        BuddyCharacter newCharacter = characterRepository.findById(request.characterSeq())
                .orElseThrow(() -> new BaseException(ResultCode.CHARACTER_NOT_FOUND));

        Member member = memberRepository.findByIdOrThrow(memberSeq);

        member.changeCharacter(newCharacter);

        return MemberResponse.from(member);
    }

    /**
     * 현재 사용 중인 버디 캐릭터의 별명(애칭)을 변경합니다.
     *
     * @param memberSeq  현재 로그인한 회원 정보
     * @param newName 새로 설정할 캐릭터의 별명
     * @throws BaseException 회원을 찾을 수 없을 경우 발생
     */
    @Transactional
    public void updateCharacterNickname(Long memberSeq, String newName) {
        Member member = memberRepository.findByIdOrThrow(memberSeq);
        member.updateCharacterNickname(newName);
        memberRepository.save(member);
    }

}
