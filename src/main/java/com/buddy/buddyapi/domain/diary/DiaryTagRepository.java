package com.buddy.buddyapi.domain.diary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryTagRepository extends JpaRepository<DiaryTag, DiaryTag.DiaryTagPK> {

}
