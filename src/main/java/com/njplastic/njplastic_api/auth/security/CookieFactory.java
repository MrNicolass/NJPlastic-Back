package com.njplastic.njplastic_api.auth.security;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Centralizes the emission of the dual-cookie pair (httpOnly
 * {@code access_token} + JS-readable {@code access_token_exp}) so login,
 * refresh and logout flows stay in sync on attributes (path, sameSite, secure).
 * Logout reuses the same factory to clear both cookies with {@code Max-Age=0}.
 */
@Component
@RequiredArgsConstructor
public class CookieFactory {

  private static final String COOKIE_ACCESS_TOKEN = "access_token";
  private static final String COOKIE_ACCESS_TOKEN_EXP = "access_token_exp";
  private static final String COOKIE_SAME_SITE = "Strict";
  private static final String COOKIE_PATH = "/";

  private final CookieProperties cookieProperties;

 /**
 * Emit the dual cookies that back the authentication contract.
 * Uses {@code addHeader} so the two {@code Set-Cookie} headers coexist on the
 * response.
 *
 * @param response servlet response receiving the headers
 * @param issued compact JWT plus its embedded exp (epoch seconds)
 * @param maxAgeSeconds cookie lifetime, matched to the JWT exp
 */
  public void writeAuthCookies(HttpServletResponse response, IssuedToken issued, long maxAgeSeconds) {
    Duration maxAge = Duration.ofSeconds(maxAgeSeconds);
    boolean secure = cookieProperties.secure();

    ResponseCookie accessToken = ResponseCookie.from(COOKIE_ACCESS_TOKEN, issued.compact())
        .httpOnly(true)
        .secure(secure)
        .sameSite(COOKIE_SAME_SITE)
        .path(COOKIE_PATH)
        .maxAge(maxAge)
        .build();
    ResponseCookie accessTokenExp = ResponseCookie.from(COOKIE_ACCESS_TOKEN_EXP, Long.toString(issued.expEpochSeconds()))
        .httpOnly(false)
        .secure(secure)
        .sameSite(COOKIE_SAME_SITE)
        .path(COOKIE_PATH)
        .maxAge(maxAge)
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, accessToken.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, accessTokenExp.toString());
  }

 /**
 * Emit empty cookies with {@code Max-Age=0} so the browser drops both
 * {@code access_token} (httpOnly, unreachable from JS) and
 * {@code access_token_exp}. Mirrors the attributes used on issuance so the
 * browser matches and removes the original cookie entries.
 *
 * @param response servlet response receiving the clearing headers
 */
  public void clearAuthCookies(HttpServletResponse response) {
    boolean secure = cookieProperties.secure();

    ResponseCookie accessToken = ResponseCookie.from(COOKIE_ACCESS_TOKEN, "")
        .httpOnly(true)
        .secure(secure)
        .sameSite(COOKIE_SAME_SITE)
        .path(COOKIE_PATH)
        .maxAge(0)
        .build();
    ResponseCookie accessTokenExp = ResponseCookie.from(COOKIE_ACCESS_TOKEN_EXP, "")
        .httpOnly(false)
        .secure(secure)
        .sameSite(COOKIE_SAME_SITE)
        .path(COOKIE_PATH)
        .maxAge(0)
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, accessToken.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, accessTokenExp.toString());
  }
}
