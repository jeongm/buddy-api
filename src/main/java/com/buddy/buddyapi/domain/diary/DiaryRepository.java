package com.buddy.buddyapi.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long>, DiaryRepositoryCustom {

    Optional<Diary> findByDiaryIdAndMember_MemberId(Long diaryId, Long memberId);

    Long deleteAllByMember_MemberId(Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Diary d WHERE d.member.memberId = :memberId")
    void bulkDeleteByMemberId(@Param("memberId") Long memberId);

}
