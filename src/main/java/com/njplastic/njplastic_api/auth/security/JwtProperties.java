package com.njplastic.njplastic_api.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT signing/issuance settings. Bound from "app.security.jwt.*" properties.
 * The secret must have at least 64 ASCII characters (RFC §6.2.1).
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
    String secret,
    long expirationMinutes,
    String issuer) {
}