package com.njplastic.njplastic_api.auth.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njplastic.njplastic_api.auth.dtos.LoginRequestDTO;
import com.njplastic.njplastic_api.auth.dtos.LoginResponseDTO;
import com.njplastic.njplastic_api.auth.dtos.PasswordResetConfirmDTO;
import com.njplastic.njplastic_api.auth.dtos.PasswordResetRequestDTO;
import com.njplastic.njplastic_api.auth.dtos.UserSummaryDTO;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.exceptions.ExpiredResetTokenException;
import com.njplastic.njplastic_api.auth.exceptions.InvalidCredentialsException;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.auth.security.CookieProperties;
import com.njplastic.njplastic_api.auth.security.IssuedToken;
import com.njplastic.njplastic_api.auth.security.JwtProperties;
import com.njplastic.njplastic_api.auth.security.JwtTokenProvider;
import com.njplastic.njplastic_api.auth.services.AuthenticationResult;
import com.njplastic.njplastic_api.auth.services.AuthenticationService;
import com.njplastic.njplastic_api.auth.services.PasswordResetService;

import io.jsonwebtoken.Claims;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthenticationControllerTest.CookieTestConfig.class)
class AuthenticationControllerTest {

  private static final String JWT_TEST_SECRET =
      "test-secret-only-used-by-junit-do-not-deploy-test-secret-only-used-by-junit";
  private static final String JWT_TEST_ISSUER = "NJPlastic-Test";

  @TestConfiguration
  static class CookieTestConfig {
    @Bean
    CookieProperties cookieProperties() {
      return new CookieProperties(false);
    }
  }

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean
  private AuthenticationService authenticationService;

  @MockitoBean
  private PasswordResetService passwordResetService;

  @MockitoBean
  private JwtTokenProvider tokenProvider;

  private String loginJson(String login, String password) throws Exception {
    return objectMapper.writeValueAsString(
        LoginRequestDTO.builder().login(login).password(password).build());
  }

  private AuthenticationResult sampleAuthResult(IssuedToken issued) {
    LoginResponseDTO response = LoginResponseDTO.builder()
        .token(issued.compact())
        .tokenType("Bearer")
        .expiresInSeconds(3600L)
        .user(UserSummaryDTO.builder()
            .id(UUID.randomUUID())
            .login("manager")
            .name("Manager Default")
            .role(UserRole.MANAGER)
            .build())
        .build();
    return new AuthenticationResult(response, issued);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @BeforeEach
  void setUpPrincipal() {
    AuthenticatedUser principal = new AuthenticatedUser(
        UUID.randomUUID(), "manager", UserRole.MANAGER, "INJECAO", "TURNO_A");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, List.of()));
  }

  @Test
  void login_returns200WithToken() throws Exception {
    AuthenticationResult result = sampleAuthResult(new IssuedToken("jwt-token", 1_700_000_000L));
    when(authenticationService.authenticate(any())).thenReturn(result);
    when(tokenProvider.expirationSeconds()).thenReturn(3600L);

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson("manager", "manager-dev-123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.user.login").value("manager"));
  }

  @Test
  void login_returns401OnInvalidCredentials() throws Exception {
    when(authenticationService.authenticate(any())).thenThrow(new InvalidCredentialsException());

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson("manager", "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid credentials"))
        .andExpect(jsonPath("$.clazzError").value("InvalidCredentialsException"));
  }

  @Test
  void login_returns400OnInvalidPayload() throws Exception {
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson("", "short")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_setsBothCookies() throws Exception {
    AuthenticationResult result = sampleAuthResult(new IssuedToken("jwt-token", 1_700_000_000L));
    when(authenticationService.authenticate(any())).thenReturn(result);
    when(tokenProvider.expirationSeconds()).thenReturn(3600L);

    MvcResult mvc = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson("manager", "manager-dev-123")))
        .andExpect(status().isOk())
        .andReturn();

    List<String> setCookies = mvc.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).hasSize(2);

    String accessToken = findCookieHeader(setCookies, "access_token=");
    assertThat(accessToken).contains("access_token=jwt-token");
    assertThat(accessToken).containsIgnoringCase("HttpOnly");
    assertThat(accessToken).contains("Path=/");
    assertThat(accessToken).contains("SameSite=Strict");
    assertThat(accessToken).contains("Max-Age=3600");
    assertThat(accessToken).doesNotContainIgnoringCase("Secure");

    String accessTokenExp = findCookieHeader(setCookies, "access_token_exp=");
    assertThat(accessTokenExp).contains("access_token_exp=1700000000");
    assertThat(accessTokenExp).doesNotContainIgnoringCase("HttpOnly");
    assertThat(accessTokenExp).contains("Path=/");
    assertThat(accessTokenExp).contains("SameSite=Strict");
    assertThat(accessTokenExp).contains("Max-Age=3600");
  }

  @Test
  void login_cookieExpMatchesJwtExp() throws Exception {
    JwtTokenProvider realProvider = new JwtTokenProvider(
        new JwtProperties(JWT_TEST_SECRET, 60, JWT_TEST_ISSUER));
    User user = User.builder()
        .id(UUID.randomUUID())
        .login("manager")
        .name("Manager Default")
        .passwordHash("hash")
        .role(UserRole.MANAGER)
        .build();
    IssuedToken realIssued = realProvider.generate(user);
    when(authenticationService.authenticate(any())).thenReturn(sampleAuthResult(realIssued));
    when(tokenProvider.expirationSeconds()).thenReturn(3600L);

    MvcResult mvc = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginJson("manager", "manager-dev-123")))
        .andExpect(status().isOk())
        .andReturn();

    MockHttpServletResponse response = mvc.getResponse();
    jakarta.servlet.http.Cookie expCookie = response.getCookie("access_token_exp");
    jakarta.servlet.http.Cookie tokenCookie = response.getCookie("access_token");
    assertThat(expCookie).isNotNull();
    assertThat(tokenCookie).isNotNull();
    assertThat(tokenCookie.getValue()).isEqualTo(realIssued.compact());

    Claims claims = realProvider.parse(realIssued.compact()).orElseThrow();
    long jwtExp = claims.getExpiration().toInstant().getEpochSecond();
    assertThat(Long.parseLong(expCookie.getValue())).isEqualTo(jwtExp);
    assertThat(Long.parseLong(expCookie.getValue())).isEqualTo(realIssued.expEpochSeconds());
  }

  @Test
  void refresh_returnsFreshTokenAndExpiration() throws Exception {
    when(tokenProvider.refresh(any(AuthenticatedUser.class)))
        .thenReturn(new IssuedToken("refreshed-token", 1_700_000_000L));
    when(tokenProvider.expirationSeconds()).thenReturn(3600L);

    mockMvc.perform(post("/auth/refresh"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("refreshed-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresInSeconds").value(3600));
  }

  @Test
  void refresh_setsBothCookies() throws Exception {
    when(tokenProvider.refresh(any(AuthenticatedUser.class)))
        .thenReturn(new IssuedToken("refreshed-token", 1_700_000_999L));
    when(tokenProvider.expirationSeconds()).thenReturn(3600L);

    MvcResult mvc = mockMvc.perform(post("/auth/refresh"))
        .andExpect(status().isOk())
        .andReturn();

    List<String> setCookies = mvc.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).hasSize(2);

    String accessToken = findCookieHeader(setCookies, "access_token=");
    assertThat(accessToken).contains("access_token=refreshed-token");
    assertThat(accessToken).containsIgnoringCase("HttpOnly");
    assertThat(accessToken).contains("Path=/");
    assertThat(accessToken).contains("SameSite=Strict");
    assertThat(accessToken).contains("Max-Age=3600");

    String accessTokenExp = findCookieHeader(setCookies, "access_token_exp=");
    assertThat(accessTokenExp).contains("access_token_exp=1700000999");
    assertThat(accessTokenExp).doesNotContainIgnoringCase("HttpOnly");
    assertThat(accessTokenExp).contains("Path=/");
    assertThat(accessTokenExp).contains("SameSite=Strict");
    assertThat(accessTokenExp).contains("Max-Age=3600");
  }

  @Test
  void requestPasswordReset_returns204AndForwardsLogin() throws Exception {
    String body = objectMapper.writeValueAsString(
        PasswordResetRequestDTO.builder().login("manager").build());

    mockMvc.perform(post("/auth/password-reset")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isNoContent());

    verify(passwordResetService).requestReset("manager");
  }

  @Test
  void requestPasswordReset_returns400WhenLoginBlank() throws Exception {
    String body = objectMapper.writeValueAsString(
        PasswordResetRequestDTO.builder().login("").build());

    mockMvc.perform(post("/auth/password-reset")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void confirmPasswordReset_returns204AndForwardsPayload() throws Exception {
    String body = objectMapper.writeValueAsString(
        PasswordResetConfirmDTO.builder().token("opaque").newPassword("new-password-here").build());

    mockMvc.perform(post("/auth/password-reset/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isNoContent());

    verify(passwordResetService).confirmReset(eq("opaque"), eq("new-password-here"));
  }

  @Test
  void confirmPasswordReset_returns400OnExpiredToken() throws Exception {
    org.mockito.Mockito.doThrow(new ExpiredResetTokenException())
        .when(passwordResetService).confirmReset(any(), any());
    String body = objectMapper.writeValueAsString(
        PasswordResetConfirmDTO.builder().token("opaque").newPassword("new-password-here").build());

    mockMvc.perform(post("/auth/password-reset/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Password reset token has expired"));
  }

  @Test
  void confirmPasswordReset_returns400WhenPayloadInvalid() throws Exception {
    String body = objectMapper.writeValueAsString(
        PasswordResetConfirmDTO.builder().token("").newPassword("short").build());

    mockMvc.perform(post("/auth/password-reset/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  private static String findCookieHeader(List<String> headers, String prefix) {
    return headers.stream()
        .filter(h -> h.startsWith(prefix))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No Set-Cookie header starting with " + prefix
            + " — headers: " + headers));
  }
}
