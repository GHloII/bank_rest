package com.example.bankcards.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Card Management API")
                        .description("""
                                <h3>Автоматизация токена для Swagger UI:</h3>
                                <script>
                                    (function() {
                                        const originalFetch = window.fetch;
                                        window.fetch = async (...args) => {
                                            const response = await originalFetch(...args);
                                            if (args[0].endsWith('/auth/login') && response.ok) {
                                                const clone = response.clone();
                                                const data = await clone.json();
                                                if (data.token) {
                                                    const ui = window.ui;
                                                    if (ui) {
                                                        ui.authActions.authorize({
                                                            bearerAuth: {
                                                                name: "bearerAuth",
                                                                schema: { type: "http", scheme: "bearer", bearerFormat: "JWT" },
                                                                value: data.token
                                                            }
                                                        });
                                                        console.log("Token automatically updated in Swagger UI");
                                                    }
                                                }
                                            }
                                            return response;
                                        };
                                    })();
                                </script>
                                """))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
