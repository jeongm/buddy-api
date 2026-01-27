package com.buddy.buddyapi.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "jwtAuth";
        // API 요청 시 보안 요구 사항 추가
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        // Security 관련 설정 (Bearer 토큰 방식 지정)
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        // 3. 서버 주소 강제 설정
        Server server = new Server();
        server.setUrl("https://buddy-api.kro.kr");
        server.setDescription("운영 서버 (HTTPS)");

        return new OpenAPI()
                .info(new Info()
                        .title("Buddy API 명세서")
                        .description("Buddy 프로젝트의 인증 및 사용자 관련 API")
                        .version("v1.0"))
                .addSecurityItem(securityRequirement)
                .components(components)
                .servers(List.of(server));
    }
}
