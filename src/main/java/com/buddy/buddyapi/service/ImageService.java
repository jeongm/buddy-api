package com.buddy.buddyapi.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) {
        if(file.isEmpty()) return null;

        try {
            // Cloudinary 업로드 호출
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "diary_uploads" // Cloudinary 내의 폴더명 지정 가능
            ));

            // 업로드 성공 후 생성된 전체 URL(https://...)을 반환
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("Cloudinary 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 저장 실패",e);
        }
    }

    public void deleteImage(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            // URL에서 Public ID 추출 (Cloudinary 삭제 시에는 파일 주소가 아니라 ID가 필요함)
            // 예: https://.../diary_uploads/abc1234.jpg -> diary_uploads/abc1234
            String publicId = extractPublicId(fileUrl);

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary 파일 삭제 성공: {}", publicId);
        } catch (Exception e) {
            log.error("Cloudinary 파일 삭제 실패: {}", e.getMessage());        }
    }

    // URL에서 public_id만 뽑아내는 헬퍼 메서드
    private String extractPublicId(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1, fileUrl.lastIndexOf("."));
    }

}
