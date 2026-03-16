package com.buddy.buddyapi.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long>, DiaryRepositoryCustom {

    Optional<Diary> findByDiarySeqAndMember_MemberSeq(Long diarySeq, Long memberSeq);

    Long deleteAllByMember_MemberSeq(Long memberSeq);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Diary d WHERE d.member.memberSeq = :memberSeq")
    void bulkDeleteByMemberSeq(@Param("memberSeq") Long memberSeq);

}
