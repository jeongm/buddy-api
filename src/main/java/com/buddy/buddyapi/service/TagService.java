package com.buddy.buddyapi.service;

import com.buddy.buddyapi.dto.response.TagResponse;
import com.buddy.buddyapi.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .map(TagResponse::from)
                .toList();
    }
}
