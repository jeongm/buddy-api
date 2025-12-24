package com.buddy.buddyapi.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tagSeq;

    @Column(unique = true, nullable = false, length = 20)
    private String name;

    public Tag(String name) {
        this.name = name;
    }


}






























