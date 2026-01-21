package com.buddy.buddyapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String uploadLocal(MultipartFile file) {
        if(file.isEmpty()) return null;

        try {
            // 1. 폴더가 없으면 생성
            File directory = new File(uploadDir);
            if (!directory.exists()) directory.mkdirs();

            // 2. 파일 이름 중복 방지 (UUID 사용)
            String originalName = file.getOriginalFilename();
            String extension = originalName.substring(originalName.lastIndexOf("."));
            String savedName = UUID.randomUUID().toString() + extension;

            // 3. 실제 파일 저장
            File destination = new File(directory.getAbsolutePath() + File.separator + savedName);
            file.transferTo(destination);

            return savedName;

        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패",e);
        }
    }

    public void deleteImage(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }

        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            File file = filePath.toFile();

            if (file.exists()) {
                file.delete();
                log.info("파일 삭제 성공: {}", fileName);
            } else {
                log.warn("파일 삭제 실패 (파일은 존재하나 삭제되지 않음): {}", fileName);
            }
        } catch (Exception e) {
            log.error("파일 삭제 중 예외 발생: {}, 사유: {}", fileName, e.getMessage(), e);
        }
    }

}
