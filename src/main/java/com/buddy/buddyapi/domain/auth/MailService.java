package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "email_verify:";
    private static final long LIMIT_TIME = 3 * 60;
    private static final String LIMIT_PREFIX = "email_limit:";

    @Async
    public void sendVerificationCode(String email) {

        String code = generateCode();
        redisTemplate.opsForValue().set(PREFIX + email, code, Duration.ofSeconds(LIMIT_TIME));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Buddy] 이메일 인증 번호입니다.");
        message.setText("인증번호: " + code + "\n3분 이내에 입력해주세요.");

        mailSender.send(message);
        log.info("이메일 발송 완료: {}", email);

    }

    public boolean verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(PREFIX + email);

        // 🚀 TODO 개선: 서비스단에서 예외를 던지도록 수정하는 것을 권장합니다.
        // 예: emailService.verifyCode() 내부에서 틀리면 throw new BaseException(...) 처리

        if (savedCode == null) {
            log.warn("인증 번호가 만료되었거나 존재하지 않음: {}", email);
            return false;
        }

        if (savedCode.equals(code)) {
            redisTemplate.delete(PREFIX + email);

            // 인증 완료 상태를 5분간 저장 (회원가입 로직에서 확인용)
            redisTemplate.opsForValue().set("verified:" + email, "true", Duration.ofMinutes(5));

            return true;
        } else {
            log.warn("인증 번호 불일치 - 입력: {}, 저장: {}", code, savedCode);
        }

        return false;
    }

    // 발송 가능 여부 확인 (동기 메서드) - 연속으로 이메일인증코드 요청하지 않도록 시간에 제한을 둔다
    public void checkSendRateLimit(String email) {
        if (redisTemplate.hasKey(LIMIT_PREFIX + email)) {
            // 1분 내에 이미 보낸 기록이 있으면 에러 뱉기
            throw new BaseException(ResultCode.TOO_MANY_REQUESTS); // 또는 429 에러
        }
        // 없으면 "나 방금 보냈어" 표시 남기기 (60초 유지)
        redisTemplate.opsForValue().set(LIMIT_PREFIX + email, "true", Duration.ofSeconds(60));
    }

    /**
     * 비밀번호 재설정 인증번호 이메일을 비동기로 발송합니다.
     */
    @Async
    public void sendPasswordResetCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[Buddy] 비밀번호 찾기 인증번호 안내");
        message.setText("안녕하세요, Buddy입니다.\n\n" +
                "비밀번호 재설정을 위한 인증번호입니다.\n" +
                "앱으로 돌아가 아래의 6자리 숫자를 입력해 주세요.\n\n" +
                "인증번호: [ " + code + " ]\n\n" +
                "* 본 인증번호는 5분 동안만 유효합니다.");

        mailSender.send(message);
    }

    private String generateCode(){
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }


}
