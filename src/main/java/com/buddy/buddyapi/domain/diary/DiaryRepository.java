package com.buddy.buddyapi.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

}
