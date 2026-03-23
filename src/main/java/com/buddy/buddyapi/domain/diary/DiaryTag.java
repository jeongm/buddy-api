package com.buddy.buddyapi.domain.diary;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "diary_tag",
        indexes = {
                @Index(name = "IX_diary_tag_tag", columnList = "tag_id")
        }
)
@IdClass(DiaryTagId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class DiaryTag {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Diary diary;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    @Builder
    public DiaryTag(Diary diary, Tag tag) {
        this.diary = diary;
        this.tag = tag;
    }


}



