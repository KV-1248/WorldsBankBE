package com.worldsbank.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI worldsBankOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WorldsBank API")
                        .description("WorldsBank Global Banking Backend API")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth"));
    }
}