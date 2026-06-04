package com.njplastic.njplastic_api.auth.security;

/**
 * Result of issuing a JWT: the compact serialization and the same {@code exp}
 * (UNIX epoch seconds) baked into the token. Exposing the exp alongside the
 * compact form removes the need to re-parse the token to derive the
 * {@code access_token_exp} cookie value (EP-FE-02 pre-req).
 */
public record IssuedToken(String compact, long expEpochSeconds) {
}
