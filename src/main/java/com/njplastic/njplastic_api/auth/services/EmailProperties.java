package com.njplastic.njplastic_api.auth.services;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-level mail settings. Bound from "app.mail.*" properties.
 * {@code from} is the envelope-from used for every outgoing message; in
 * dev this points to a Mailpit instance, in prod it must match the SMTP
 * relay's authenticated identity. {@code passwordResetTtlMinutes} caps the
 * lifetime of recovery tokens.
 */
@ConfigurationProperties(prefix = "app.mail")
public record EmailProperties(
    String from,
    String passwordResetBaseUrl,
    int passwordResetTtlMinutes) {
}
