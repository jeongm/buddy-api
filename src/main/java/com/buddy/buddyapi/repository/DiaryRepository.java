package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.entity.Diary;
import com.buddy.buddyapi.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 특정 맴버의 전체 일기
    List<Diary> findByMember(Member member);
    
    //TODO 특정 회원의 특정 기간 사이 일기 목록 조회(날짜 필터링용)
    // 왜 맴버를 통으로 하지?
    List<Diary> findAllByMemberAndCreatedAtBetweenOrderByCreatedAtDesc(
            Member member,
            LocalDateTime start,
            LocalDateTime end
    );

    // TODO 일기 상세 조회 시 작성자 확인용(보안)
    // findById로 가능 아예 Member와 함께 조회하면 권한 체크로 더 낫나??
    Optional<Diary> findByDiarySeqAndMember(Long diarySeq, Member member);


}
