package com.buddy.buddyapi.domain.auth;

import com.buddy.buddyapi.domain.auth.dto.EmailRequest;
import com.buddy.buddyapi.domain.member.MemberRepository;
import com.buddy.buddyapi.global.exception.BaseException;
import com.buddy.buddyapi.global.exception.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final MemberRepository memberRepository;

    private static final String PREFIX = "email_verify:";
    private static final long LIMIT_TIME = 3 * 60;

    public void sendVerificationCode(String email) {

        String code = String.valueOf((int) (Math.random() * 8999999) + 100000);

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

        if (savedCode == null) {
            log.warn("인증 번호가 만료되었거나 존재하지 않음: {}", email);
            return false;
        }
        if (!savedCode.equals(code)) {
            log.warn("인증 번호 불일치 - 입력: {}, 저장: {}", code, savedCode);
            return false;
        }

        redisTemplate.delete(PREFIX + email);
        return true;
    }


}
