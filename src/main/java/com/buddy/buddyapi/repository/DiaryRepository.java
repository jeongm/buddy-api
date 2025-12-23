package com.buddy.buddyapi.repository;

import com.buddy.buddyapi.entity.Diary;
import com.buddy.buddyapi.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    List<Diary> findByMember(Member member);
    
    //TODO 특정유저의 특정일 기록들

    // TODO 특정일 기록중 하나
}
