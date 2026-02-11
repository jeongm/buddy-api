package com.buddy.buddyapi.global.config;

import com.buddy.buddyapi.global.security.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final HandlerExceptionResolver exceptionResolver;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauthSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauthFailureHandler;


    public SecurityConfig (
            JwtTokenProvider jwtTokenProvider,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2AuthenticationSuccessHandler oauthSuccessHandler, OAuth2AuthenticationFailureHandler oauthFailureHandler) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.exceptionResolver = exceptionResolver;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oauthSuccessHandler = oauthSuccessHandler;
        this.oauthFailureHandler = oauthFailureHandler;
    }



    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화(JWT사용시 필수)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 미사용
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/login/oauth2/**", "/oauth2/**").permitAll()
                        .requestMatchers("/images/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // OPTIONS 메서드는 CORS Preflight를 위해 모두 허용(OPTIONS 메서드로 들어오는 모든 예비 요청은 '인증 없이' 통과)
                        .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization")
                        )
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oauthSuccessHandler)
                        .failureHandler(oauthFailureHandler)
                )
                // UsernamePasswordAuthenticationFilter 이전에 JWT 필터 실행
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, exceptionResolver),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // TODO 배포시...변경...?
        // 주소는 미확정이므로
        configuration.setAllowedOriginPatterns(java.util.List.of("*"));
//        configuration.setAllowedOrigins(java.util.List.of("https://buddydiary.vercel.app", "http://localhost:8080"));

        // 브라우저가 보낼 모든 메서드 허용
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 프론트에서 보낼 헤더 허용 (Authorization 포함)
//        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "Cache-Control"));
        configuration.setAllowedHeaders(java.util.List.of("*"));

        // 프론트가 내 응답에서 Authorization 헤더(JWT를 읽을 수 있게 노출)
        configuration.setExposedHeaders(java.util.List.of("Authorization"));

        // 자격 증명(쿠키, 인증 헤더) 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;

    }


}
