package com.buddy.buddyapi.global.infra;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) {
        if(file.isEmpty()) return null;

        try {
            // Cloudinary 업로드 호출
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", "diary_uploads") // Cloudinary 내의 폴더명 지정 가능
             );

            // 업로드 성공 후 생성된 전체 URL(https://...)을 반환
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("Cloudinary 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 저장 실패", e);
        }
    }

    public void deleteImage(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty())
            return;

        try {
            // URL에서 Public ID 추출 (Cloudinary 삭제 시에는 파일 주소가 아니라 ID가 필요함)
            // 예: https://.../diary_uploads/abc1234.jpg -> diary_uploads/abc1234
            String publicId = extractPublicId(fileUrl);

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary 파일 삭제 성공: {}", publicId);
        } catch (Exception e) {
            log.error("Cloudinary 파일 삭제 실패: {}", e.getMessage());
        }
    }

    /**
     * Cloudinary URL에서 public_id를 추출합니다.
     * URL 예시: https://res.cloudinary.com/cloud/image/upload/v1234/diary_uploads/abc.jpg
     * public_id: diary_uploads/abc  (폴더 경로 포함, 확장자 제외)
     */
    private String extractPublicId(String fileUrl) {
        String[] splitByUpload = fileUrl.split("/upload/");
        if (splitByUpload.length < 2) {
            log.warn("Cloudinary URL 파싱 실패 (예상치 못한 형식): {}", fileUrl);
            return fileUrl;
        }
        // "v1234567/diary_uploads/abc.jpg" → "diary_uploads/abc.jpg"
        String withoutVersion = splitByUpload[1].replaceFirst("v\\d+/", "");
        // "diary_uploads/abc.jpg" → "diary_uploads/abc"
        int lastDot = withoutVersion.lastIndexOf(".");
        return lastDot > 0 ? withoutVersion.substring(0, lastDot) : withoutVersion;
    }
}
