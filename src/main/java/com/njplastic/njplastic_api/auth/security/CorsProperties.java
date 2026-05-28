package com.njplastic.njplastic_api.auth.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS settings bound from "app.security.cors.*". The frontend origin list
 * is restricted to the Controller's domain in production (RFC §6.4 - A05).
 */
@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(List<String> allowedOrigins) {
}