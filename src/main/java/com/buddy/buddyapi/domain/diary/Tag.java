package com.buddy.buddyapi.domain.diary;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_seq")
    private Long tagSeq;

    @Column(unique = true, nullable = false, length = 20)
    private String name;

    public Tag(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // getClass() 대신 instanceof 를 사용해야 프록시 객체와 정상 비교가 됩니다.
        if (!(o instanceof Tag tag)) return false;

        // 필드 직접 접근(tag.name) 대신 getter(tag.getName()) 사용 필수!
        return Objects.equals(this.getName(), tag.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getName());
    }


}






























