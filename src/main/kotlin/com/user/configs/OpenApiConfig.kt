package com.user.configs

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    @Value("\${spring.application.name}") private val applicationName: String,
    @Value("\${server.port}") private val serverPort: String,
) {

    companion object {
        const val BEARER_SCHEME = "bearerAuth"
    }

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("$applicationName service API")
                .description(
                    "REST API сервиса пользователей: профили, поиск и контакты. " +
                        "Аутентификация — JWT access-токен в заголовке Authorization: Bearer <token>."
                )
                .version("v1")
                .contact(Contact().name("User service"))
                .license(License().name("Proprietary"))
        )
        .servers(
            listOf(
                Server().url("http://localhost:$serverPort").description("Локальная разработка")
            )
        )
        .components(
            Components().addSecuritySchemes(
                BEARER_SCHEME,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT access-токен, выданный AuthService")
            )
        )
        .addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))

}
