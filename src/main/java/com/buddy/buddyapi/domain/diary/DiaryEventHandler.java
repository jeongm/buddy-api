package com.buddy.buddyapi.domain.diary;

import com.buddy.buddyapi.domain.diary.event.DiaryImageUpdateEvent;
import com.buddy.buddyapi.domain.diary.event.DiaryImagesCleanupEvent;
import com.buddy.buddyapi.domain.member.event.MemberWithdrawEvent;
import com.buddy.buddyapi.global.infra.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiaryEventHandler {

    private final DiaryRepository diaryRepository;

    private final ImageService imageService;

    private final ApplicationEventPublisher eventPublisher;


    /**
     * [트랜잭션 커밋 후 실행]
     * 일기 수정 시 새 이미지를 Cloudinary에 업로드하고 DB URL을 갱신합니다.
     * 기존 이미지가 있으면 함께 삭제합니다.
     * REQUIRES_NEW로 새 트랜잭션을 시작하여 DB URL 갱신을 보장합니다.
     *
     * @param event 일기 이미지 업데이트 이벤트 (diaryId, oldImageUrl, newImage)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDiaryImageUpdate(DiaryImageUpdateEvent event) {
        String newImageUrl = imageService.uploadImage(event.newImage());
        diaryRepository.updateImageUrl(event.diaryId(), newImageUrl);
        if (event.oldImageUrl() != null) {
            imageService.deleteImage(event.oldImageUrl());
        }
    }

    /**
     * [회원 탈퇴] DB 삭제 전 Cloudinary 이미지 URL을 수집하고 정리 이벤트를 예약합니다.
     *
     * @EventListener 는 즉시 동기 실행되므로, memberRepository.deleteById() 보다 먼저 실행됩니다.
     * 덕분에 아직 살아있는 Diary에서 URL을 안전하게 읽을 수 있습니다.
     * Diary의 DB 삭제는 Member 삭제 시 @OnDelete(CASCADE)가 처리합니다.
     */
    @EventListener
    @Transactional(readOnly = true)
    public void handleMemberWithdraw(MemberWithdrawEvent event) {
        Long memberId = event.memberId();

        List<String> imageUrls = diaryRepository.findImageUrlsByMemberId(memberId);

        if (!imageUrls.isEmpty()) {
            // 트랜잭션 커밋 후 Cloudinary 삭제를 위해 이벤트 예약
            eventPublisher.publishEvent(new DiaryImagesCleanupEvent(imageUrls));
        }

        log.info("[DiaryService] 탈퇴 유저(Id: {})의 Cloudinary 이미지 {}개 삭제 예약",
                memberId, imageUrls.size());
    }

    /**
     * [트랜잭션 커밋 후 실행]
     * Cloudinary 이미지 URL 목록을 받아 일괄 삭제합니다.
     * 개별 실패가 전체 삭제를 막지 않도록 예외를 개별 흡수합니다.
     *
     * @param event 삭제할 이미지 URL 목록이 담긴 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDiaryImagesCleanup(DiaryImagesCleanupEvent event) {
        event.imageUrls().forEach(url -> {
            try {
                imageService.deleteImage(url);
            } catch (Exception e) {
                log.error("📢 [DiaryEventHandler] Cloudinary 이미지 삭제 실패: {}", url, e);
            }
        });
    }
}
