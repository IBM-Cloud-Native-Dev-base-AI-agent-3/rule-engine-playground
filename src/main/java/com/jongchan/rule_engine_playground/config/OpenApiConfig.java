package com.jongchan.rule_engine_playground.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        // Use a relative server URL so Swagger UI uses the same origin as the UI page (works with ngrok)
        openAPI.setServers(List.of(new Server().url("/")));
        return openAPI;
    }
}

