package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.entity.Diary;
import com.buddy.buddyapi.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 특정 맴버의 전체 일기
    List<Diary> findByMember(Member member);
    
    // 왜 맴버를 통으로 하지?
    @Query(
            "SELECT DISTINCT d FROM Diary d " +
            "LEFT JOIN FETCH d.diaryTags dt " +
            "LEFT JOIN FETCH dt.tag " +
            "WHERE d.member = :member " +
            "AND d.createdAt BETWEEN :start AND :end " +
            "ORDER BY d.createdAt DESC"
    )
    List<Diary> findAllByMemberAndCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("member") Member member,
            @Param("start") LocalDateTime start,
            @Param("end")LocalDateTime end
    );

    // findById로 가능 아예 Member와 함께 조회하면 권한 체크로 더 낫나??
    Optional<Diary> findByDiarySeqAndMember(Long diarySeq, Member member);


}
