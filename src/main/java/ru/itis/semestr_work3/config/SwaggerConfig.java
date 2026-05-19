package ru.itis.semestr_work3.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aura Motum — Car Rental API")
                        .description("REST API платформы аренды автомобилей Aura Motum. " +
                                "Поддерживаются два способа аутентификации (любой на выбор): " +
                                "Basic Auth (для Postman/curl) или сессионный cookie JSESSIONID (для браузера).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Aura Motum")
                                .email("support@auramotum.ru")))

                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))

                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic Authentication. Используется в Postman и curl."))
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Сессионный cookie. Устанавливается автоматически после логина через /login.")));
    }
}