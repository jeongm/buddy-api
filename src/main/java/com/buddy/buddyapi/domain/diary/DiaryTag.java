package com.buddy.buddyapi.domain.diary;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "diary_tag",
        indexes = {
                @Index(name = "IX_diary_tag_tag", columnList = "tag_id")
        }
)
public class DiaryTag {

    @EmbeddedId
    private DiaryTagPK diaryTagPK;

    @MapsId("diaryId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Diary diary;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    @EqualsAndHashCode
    public static class DiaryTagPK implements Serializable {
        private Long diaryId;
        private Long tagId;
    }

    @Builder
    public DiaryTag(Diary diary, Tag tag) {
        this.diaryTagPK = new DiaryTagPK(diary.getDiaryId(), tag.getTagId());
        this.diary = diary;
        this.tag = tag;
    }


}



