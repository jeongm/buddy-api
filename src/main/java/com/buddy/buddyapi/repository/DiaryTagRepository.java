package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.entity.Diary;
import com.buddy.buddyapi.entity.DiaryTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryTagRepository extends JpaRepository<DiaryTag, DiaryTag.DiaryTagPK> {

    List<DiaryTag> findByDiary(Diary diary);
}
