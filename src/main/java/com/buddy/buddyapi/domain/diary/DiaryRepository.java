package com.buddy.buddyapi.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long>, DiaryRepositoryCustom {

    Optional<Diary> findByDiarySeqAndMember_MemberSeq(Long diarySeq, Long memberSeq);



}
