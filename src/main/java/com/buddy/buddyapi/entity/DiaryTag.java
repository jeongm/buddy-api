package com.buddy.buddyapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "diary_tag")
public class DiaryTag {

    @EmbeddedId
    private DiaryTagPK diaryTagPK = new DiaryTagPK();

    @MapsId("diarySeq")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_seq")
    private Diary diary;

    @MapsId("tagSeq")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_seq")
    private Tag tag;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class DiaryTagPK implements Serializable {
        private Long diarySeq;
        private Long tagSeq;
    }

    @Builder
    public DiaryTag(Diary diary, Tag tag) {
        this.diary = diary;
        this.tag = tag;
        this.diaryTagPK = new DiaryTagPK();
    }


}



