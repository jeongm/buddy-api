package com.buddy.buddyapi.domain.diary;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DiaryTagId implements Serializable {
    private Long diary;
    private Long tag;
}
