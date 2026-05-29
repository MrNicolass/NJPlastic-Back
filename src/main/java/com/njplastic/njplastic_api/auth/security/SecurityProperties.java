package com.njplastic.njplastic_api.auth.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Public (unauthenticated) endpoints bound from "app.security.public-paths".
 * Replaces the previously hardcoded allow-list; override entirely via the
 * SECURITY_PUBLIC_PATHS environment variable (RFC §6.2, EP-BE-02).
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(List<String> publicPaths) {
}
