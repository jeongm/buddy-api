package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.request.DiaryCreateRequest;
import com.buddy.buddyapi.dto.request.DiaryUpdateRequest;
import com.buddy.buddyapi.dto.response.DiaryDetailResponse;
import com.buddy.buddyapi.dto.response.DiaryListResponse;
import com.buddy.buddyapi.entity.Diary;
import com.buddy.buddyapi.entity.Member;
import com.buddy.buddyapi.entity.Tag;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import com.buddy.buddyapi.repository.DiaryRepository;
import com.buddy.buddyapi.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final TagRepository tagRepository;

    // 특정 날짜의 일기 목록 조회
    public List<DiaryListResponse> getDiariesByDate(Member member, LocalDate date) {
        // 해당 날짜의 00:00:00
        LocalDateTime start = date.atStartOfDay();
        // 해당 날짜의 23:59:59.999999
        LocalDateTime end = date.atTime(LocalTime.MAX);

        return diaryRepository.findAllByMemberAndCreatedAtBetweenOrderByCreatedAtDesc(member, start, end)
                .stream()
                .map(DiaryListResponse::from) // DTO에 만든 from 메서드 활용
                .toList();
    }

    // 일기 저장 (Create)
    @Transactional
    public Long createDiary(Member member, DiaryCreateRequest request) {
        // 1. 일기 엔티티 생성
        Diary diary = Diary.builder()
                .title(request.title())
                .content(request.content())
                .imageUrl(request.imageUrl())
                .member(member)
                .build();

        // 2. 태그 리스트가 있다면 조회 후 연결
        if (request.tagSeqs() != null && !request.tagSeqs().isEmpty()) {
            List<Tag> tags = tagRepository.findAllByTagSeqIn(request.tagSeqs());

            // 요청한 개수와 DB에서 찾은 개수가 다르면 예외 처리 (선택사항)
            if (tags.size() != request.tagSeqs().size()) {
                throw new BaseException(ResultCode.TAG_NOT_FOUND);
            }

            diary.addTags(tags);
        }

        return diaryRepository.save(diary).getDiarySeq();
    }

    @Transactional
    public void updateDiary(Member member, Long diarySeq, DiaryUpdateRequest request) {
        // 1. 본인의 일기인지 확인하며 조회
        Diary diary = diaryRepository.findByDiarySeqAndMember(diarySeq, member)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        // 2. 기본 정보 수정
        diary.updateDiary(request.title(), request.content(), request.imageUrl());

        // 3. 태그 교체
        if (request.tagSeqs() != null) {
            List<Tag> tags = tagRepository.findAllById(request.tagSeqs());
            diary.updateTags(tags);
        }
    }

    @Transactional
    public void deleteDiary(Member member, Long diarySeq) {
        Diary diary = diaryRepository.findByDiarySeqAndMember(diarySeq, member)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        diaryRepository.delete(diary);
        // diary_tag 테이블의 데이터도 알아서 같이 지워집니다!
    }

    @Transactional(readOnly = true)
    public DiaryDetailResponse getDiaryDetail(Member member, Long diarySeq) {
        Diary diary = diaryRepository.findByDiarySeqAndMember(diarySeq, member)
                .orElseThrow(() -> new BaseException(ResultCode.DIARY_NOT_FOUND));

        return DiaryDetailResponse.from(diary);
    }

}
