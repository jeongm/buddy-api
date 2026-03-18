package com.buddy.buddyapi.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            // resources 폴더에 다운받은 json 파일 이름
            InputStream serviceAccount = new ClassPathResource("firebase-buddyapp-account.json").getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase 연동 성공");
            }
        } catch (Exception e) {
            log.error("Firebase 초기화 중 에러 발생", e);
        }
    }
}
