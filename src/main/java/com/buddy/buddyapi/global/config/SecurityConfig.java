package com.buddy.buddyapi.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화(JWT사용시 필수)
                .headers(headers -> headers.frameOptions(options -> options.disable()))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                ) // 모든 요청 허용
                // TODO 기능 완성 후 루트 설정
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 미사용
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/v1/auth/**").permitAll() // 명세서의 auth 경로는 모두 허용
//                        .requestMatchers(
//                                "/v3/api-docs/**",
//                                "/swagger-ui/**",
//                                "/swagger-ui.html",
//                                "/swagger-resources/**",
//                                "/webjars/**"
//                        ).permitAll()                        .requestMatchers("/h2-console/**").permitAll() // H2 콘솔 접근 허용
//                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
//                )
                // UsernamePasswordAuthenticationFilter 이전에 JWT 필터 실행
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}
