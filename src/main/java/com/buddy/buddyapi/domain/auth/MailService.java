package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.domain.auth.enums.EmailPurpose;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    // 키 Prefix를 목적에 따라 동적으로 생성하기 위해 상수 변경
    private static final String CODE_PREFIX = "email_code:";
    private static final String TOKEN_PREFIX = "email_token:";
    private static final String LIMIT_PREFIX = "email_limit:";

    private static final long CODE_LIMIT_TIME_MINUTES = 5;  // 인증번호 입력 제한 시간
    private static final long TOKEN_LIMIT_TIME_MINUTES = 30; // 티켓(UUID) 유효 시간


    /**
     * [공통] 목적에 맞는 인증 코드를 생성하여 Redis에 저장하고 이메일로 발송합니다.
     */
    @Async
    public void sendCode(String email, EmailPurpose purpose) {
        String code = generateCode();
        String redisKey = getCodeKey(email, purpose);

        // Redis에 5분간 저장
        redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(CODE_LIMIT_TIME_MINUTES));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);

        if (purpose == EmailPurpose.SIGNUP) {
            message.setSubject("[Buddy] 회원가입 이메일 인증 번호입니다.");
            message.setText("Buddy에 오신 것을 환영합니다!\n\n인증번호: [ " + code + " ]\n5분 이내에 입력해주세요.");
        } else if (purpose == EmailPurpose.PASSWORD_RESET) {
            message.setSubject("[Buddy] 비밀번호 찾기 인증번호 안내");
            message.setText("안녕하세요, Buddy입니다.\n\n비밀번호 재설정을 위한 인증번호입니다.\n인증번호: [ " + code + " ]\n5분 이내에 입력해주세요.");
        }

        mailSender.send(message);
        log.info("[{}] 이메일 발송 완료: {}", purpose.name(), email);
    }

    /**
     * [공통] 사용자가 입력한 코드를 검증하고, 성공 시 UUID 티켓을 발급합니다.
     * @return 검증 성공 시 발급되는 일회용 UUID 티켓
     */
    public String verifyCodeAndGetToken(String email, String code, EmailPurpose purpose) {
        String redisKey = getCodeKey(email, purpose);
        String savedCode = redisTemplate.opsForValue().get(redisKey);

        if (savedCode == null || !savedCode.equals(code)) {
            log.warn("[{}] 인증 번호 불일치 또는 만료 - 이메일: {}, 입력: {}, 저장: {}", purpose.name(), email, code, savedCode);
            throw new BaseException(ResultCode.INVALID_CODE);
        }

        redisTemplate.delete(redisKey);

        String verificationToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(getTokenKey(email, purpose),verificationToken, Duration.ofMinutes(TOKEN_LIMIT_TIME_MINUTES));

        log.info("[{}] 이메일 인증 성공 및 티켓 발급 완료: {}", purpose.name(), email);
        return  verificationToken;
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

    private String generateCode(){
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }

    // 목적(Purpose)이 포함된 Redis Key 생성기
    private String getCodeKey(String email, EmailPurpose purpose) {
        return CODE_PREFIX + purpose.name() + ":" + email;
    }

    private String getTokenKey(String email, EmailPurpose purpose) {
        return TOKEN_PREFIX + purpose.name() + ":" + email;
    }


}
