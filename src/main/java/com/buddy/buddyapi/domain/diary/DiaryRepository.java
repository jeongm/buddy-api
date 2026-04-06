package com.buddy.buddyapi.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long>, DiaryRepositoryCustom {

    Optional<Diary> findByDiaryIdAndMember_MemberId(Long diaryId, Long memberId);

    @Query("SELECT d.imageUrl FROM Diary d " +
            "WHERE d.member.memberId = :memberId AND d.imageUrl IS NOT NULL")
    List<String> findImageUrlsByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("UPDATE Diary d SET d.imageUrl = :imageUrl WHERE d.diaryId = :diaryId")
    void updateImageUrl(@Param("diaryId") Long diaryId, @Param("imageUrl") String imageUrl);

    // 스트릭 재계산용: 무거운 일기 본문은 버리고, 날짜만 중복 없이 최신순으로 가져옴
    @Query("SELECT DISTINCT d.diaryDate FROM Diary d WHERE d.member.memberId = :memberId ORDER BY d.diaryDate DESC")
    List<LocalDate> findDistinctDiaryDatesDescByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 일기의 session_id를 NULL로 업데이트합니다.
     * 일기 삭제 전 FK 제약조건 위반 방지를 위해 호출합니다.
     *
     * @param diaryId 대상 일기 PK
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Diary d SET d.chatSession = NULL WHERE d.diaryId = :diaryId")
    void detachSession(@Param("diaryId") Long diaryId);

}
