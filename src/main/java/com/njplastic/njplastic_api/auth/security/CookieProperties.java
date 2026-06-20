package com.njplastic.njplastic_api.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentication cookie settings bound from "app.security.cookies.*". The
 * {@code secure} flag toggles the {@code Secure} attribute on the dual cookies
 * issued by {@code /auth/login} and {@code /auth/refresh}: true in production
 * (HTTPS only) and false in dev (so local Next.js on HTTP can read them).
 * SameSite and Path are fixed in code (Strict, "/") because they are part of
 * the contract with the frontend, not a per-deploy concern (pre-req).
 */
@ConfigurationProperties(prefix = "app.security.cookies")
public record CookieProperties(boolean secure) {
}
