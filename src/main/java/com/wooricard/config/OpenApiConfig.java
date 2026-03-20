package com.wooricard.config;

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
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wooricard API Gateway")
                        .description("""
                                ## 우리카드 API Gateway

                                모든 서비스 요청의 단일 진입점입니다.

                                ### 인증 방법
                                1. `POST /auth/token` 으로 JWT 토큰 발급
                                2. 우측 상단 **Authorize** 버튼 클릭 후 `Bearer {발급받은_토큰}` 입력
                                3. 이후 모든 요청에 자동으로 토큰이 포함됩니다.

                                ### 라우팅 구조
                                | 경로 | 대상 서비스 | 포트 |
                                |------|------------|------|
                                | `/api/authorizations/**` | 승인/결제 서비스 | 8081 |
                                | `/api/settlement/**` | 정산 서비스 | 8082 |
                                | `/api/billing/**` | 매입 청구 서비스 | 8083 |
                                """)
                        .version("v1.0.0"))
                .servers(List.of(
                        new Server().url("http://192.168.1.249:8080").description("Gateway 서버")
                ))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("POST /auth/token 으로 발급받은 JWT 토큰을 입력하세요.")));
    }
}
