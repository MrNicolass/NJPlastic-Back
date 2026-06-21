package com.njplastic.njplastic_api.auth.services;

import com.njplastic.njplastic_api.auth.dtos.LoginResponseDTO;
import com.njplastic.njplastic_api.auth.security.IssuedToken;

/**
 * Aggregated outcome of {@link AuthenticationService#authenticate}: the
 * JSON payload returned to the client plus the freshly issued token. The
 * controller needs both - the DTO becomes the response body and the
 * {@link IssuedToken} drives the {@code access_token}/{@code access_token_exp}
 * cookies emitted in parallel (pre-req).
 */
public record AuthenticationResult(LoginResponseDTO response, IssuedToken issued) {
}
