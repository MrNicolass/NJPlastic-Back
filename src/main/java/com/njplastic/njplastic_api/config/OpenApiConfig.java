package com.njplastic.njplastic_api.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(info = @Info(title = "NJPlastic API", version = "1.0.0", description = "Web-IoT platform for monitoring plastic injection machines. "
    + "Captures production cycle pulses via MQTT, processes them in the Spring Boot backend "
    + "and exposes REST endpoints for the React/Next.js dashboard.", contact = @Contact(name = "NJPlastic Team", email = "contato@njplastic.com.br"), license = @License(name = "Proprietary")), servers = {
        @Server(url = "http://localhost:8111", description = "Local development")
    })
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", in = SecuritySchemeIn.HEADER, description = "JWT Bearer token issued by POST /auth/login (EP-BE-02).")
public class OpenApiConfig {
}