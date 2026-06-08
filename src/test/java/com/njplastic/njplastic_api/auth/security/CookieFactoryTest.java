package com.njplastic.njplastic_api.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieFactoryTest {

  private static final String ACCESS_TOKEN_PREFIX = "access_token=";
  private static final String ACCESS_TOKEN_EXP_PREFIX = "access_token_exp=";

  @Test
  void writeAuthCookies_emitsBothCookiesWithSharedAttributes() {
    CookieFactory factory = new CookieFactory(new CookieProperties(false));
    MockHttpServletResponse response = new MockHttpServletResponse();
    IssuedToken issued = new IssuedToken("jwt-token-abc", 1_700_000_000L);

    factory.writeAuthCookies(response, issued, 3600L);

    List<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(headers).hasSize(2);

    String accessToken = findCookieHeader(headers, ACCESS_TOKEN_PREFIX);
    assertThat(accessToken).contains("access_token=jwt-token-abc");
    assertThat(accessToken).containsIgnoringCase("HttpOnly");
    assertThat(accessToken).contains("Path=/");
    assertThat(accessToken).contains("SameSite=Strict");
    assertThat(accessToken).contains("Max-Age=3600");
    assertThat(accessToken).doesNotContainIgnoringCase("Secure");

    String accessTokenExp = findCookieHeader(headers, ACCESS_TOKEN_EXP_PREFIX);
    assertThat(accessTokenExp).contains("access_token_exp=1700000000");
    assertThat(accessTokenExp).doesNotContainIgnoringCase("HttpOnly");
    assertThat(accessTokenExp).contains("Path=/");
    assertThat(accessTokenExp).contains("SameSite=Strict");
    assertThat(accessTokenExp).contains("Max-Age=3600");
  }

  @Test
  void writeAuthCookies_emitsSecureWhenEnabled() {
    CookieFactory factory = new CookieFactory(new CookieProperties(true));
    MockHttpServletResponse response = new MockHttpServletResponse();

    factory.writeAuthCookies(response, new IssuedToken("token", 1_700_000_999L), 60L);

    List<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(findCookieHeader(headers, ACCESS_TOKEN_PREFIX)).containsIgnoringCase("Secure");
    assertThat(findCookieHeader(headers, ACCESS_TOKEN_EXP_PREFIX)).containsIgnoringCase("Secure");
  }

  @Test
  void writeAuthCookies_omitsSecureWhenDisabled() {
    CookieFactory factory = new CookieFactory(new CookieProperties(false));
    MockHttpServletResponse response = new MockHttpServletResponse();

    factory.writeAuthCookies(response, new IssuedToken("token", 1_700_000_999L), 60L);

    List<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(findCookieHeader(headers, ACCESS_TOKEN_PREFIX)).doesNotContainIgnoringCase("Secure");
    assertThat(findCookieHeader(headers, ACCESS_TOKEN_EXP_PREFIX)).doesNotContainIgnoringCase("Secure");
  }

  @Test
  void writeAuthCookies_propagatesArbitraryMaxAge() {
    CookieFactory factory = new CookieFactory(new CookieProperties(false));
    MockHttpServletResponse response = new MockHttpServletResponse();

    factory.writeAuthCookies(response, new IssuedToken("token", 1_700_000_000L), 7200L);

    List<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(findCookieHeader(headers, ACCESS_TOKEN_PREFIX)).contains("Max-Age=7200");
    assertThat(findCookieHeader(headers, ACCESS_TOKEN_EXP_PREFIX)).contains("Max-Age=7200");
  }

  @Test
  void clearAuthCookies_emitsEmptyCookiesWithMaxAgeZero() {
    CookieFactory factory = new CookieFactory(new CookieProperties(false));
    MockHttpServletResponse response = new MockHttpServletResponse();

    factory.clearAuthCookies(response);

    List<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(headers).hasSize(2);

    String accessToken = findCookieHeader(headers, ACCESS_TOKEN_PREFIX);
    assertThat(accessToken).contains("access_token=;");
    assertThat(accessToken).containsIgnoringCase("HttpOnly");
    assertThat(accessToken).contains("Path=/");
    assertThat(accessToken).contains("SameSite=Strict");
    assertThat(accessToken).contains("Max-Age=0");
    assertThat(accessToken).doesNotContainIgnoringCase("Secure");

    String accessTokenExp = findCookieHeader(headers, ACCESS_TOKEN_EXP_PREFIX);
    assertThat(accessTokenExp).contains("access_token_exp=;");
    assertThat(accessTokenExp).doesNotContainIgnoringCase("HttpOnly");
    assertThat(accessTokenExp).contains("Path=/");
    assertThat(accessTokenExp).contains("SameSite=Strict");
    assertThat(accessTokenExp).contains("Max-Age=0");
  }

  @Test
  void clearAuthCookies_emitsSecureWhenEnabled() {
    CookieFactory factory = new CookieFactory(new CookieProperties(true));
    MockHttpServletResponse response = new MockHttpServletResponse();

    factory.clearAuthCookies(response);

    List<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(findCookieHeader(headers, ACCESS_TOKEN_PREFIX)).containsIgnoringCase("Secure");
    assertThat(findCookieHeader(headers, ACCESS_TOKEN_EXP_PREFIX)).containsIgnoringCase("Secure");
  }

  private static String findCookieHeader(List<String> headers, String prefix) {
    return headers.stream()
        .filter(h -> h.startsWith(prefix))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No Set-Cookie header starting with " + prefix
            + " — headers: " + headers));
  }
}
