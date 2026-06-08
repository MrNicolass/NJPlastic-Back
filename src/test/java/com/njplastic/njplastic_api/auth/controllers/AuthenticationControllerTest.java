package com.njplastic.njplastic_api.auth.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
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
import com.njplastic.njplastic_api.auth.security.CookieFactory;
import com.njplastic.njplastic_api.auth.security.CookieProperties;
import com.njplastic.njplastic_api.auth.security.IssuedToken;
import com.njplastic.njplastic_api.auth.security.JwtProperties;
import com.njplastic.njplastic_api.auth.security.JwtTokenProvider;
import com.njplastic.njplastic_api.auth.services.AuthenticationResult;
import com.njplastic.njplastic_api.auth.services.AuthenticationService;
import com.njplastic.njplastic_api.auth.services.PasswordResetService;
import com.njplastic.njplastic_api.auth.services.UserService;

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

    @Bean
    CookieFactory cookieFactory(CookieProperties cookieProperties) {
      return new CookieFactory(cookieProperties);
    }

    @Bean
    org.springframework.web.servlet.config.annotation.WebMvcConfigurer authenticationPrincipalResolverConfigurer() {
      return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
        @Override
        public void addArgumentResolvers(
            java.util.List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
          resolvers.add(
              new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver());
        }
      };
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
  private UserService userService;

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
  void me_returnsUserSummaryFromPrincipalId() throws Exception {
    AuthenticatedUser principal = (AuthenticatedUser) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    User user = User.builder()
        .id(principal.id())
        .login("manager")
        .name("Manager Default")
        .email("manager@njplastic.com")
        .passwordHash("hash")
        .role(UserRole.MANAGER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .active(true)
        .build();
    when(userService.findById(principal.id())).thenReturn(Optional.of(user));

    mockMvc.perform(get("/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(principal.id().toString()))
        .andExpect(jsonPath("$.login").value("manager"))
        .andExpect(jsonPath("$.name").value("Manager Default"))
        .andExpect(jsonPath("$.role").value("MANAGER"))
        .andExpect(jsonPath("$.sector").value("INJECAO"))
        .andExpect(jsonPath("$.shift").value("TURNO_A"));

    verify(userService).findById(principal.id());
  }

  @Test
  void me_returns404WhenPrincipalNoLongerExists() throws Exception {
    when(userService.findById(any(UUID.class))).thenReturn(Optional.empty());

    mockMvc.perform(get("/auth/me"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User not found"))
        .andExpect(jsonPath("$.clazzError").value("UserNotFoundException"));
  }

  @Test
  void logout_returns204AndClearsBothCookies() throws Exception {
    MvcResult mvc = mockMvc.perform(post("/auth/logout"))
        .andExpect(status().isNoContent())
        .andReturn();

    List<String> setCookies = mvc.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).hasSize(2);

    String accessToken = findCookieHeader(setCookies, "access_token=");
    assertThat(accessToken).contains("access_token=;");
    assertThat(accessToken).containsIgnoringCase("HttpOnly");
    assertThat(accessToken).contains("Path=/");
    assertThat(accessToken).contains("SameSite=Strict");
    assertThat(accessToken).contains("Max-Age=0");
    assertThat(accessToken).doesNotContainIgnoringCase("Secure");

    String accessTokenExp = findCookieHeader(setCookies, "access_token_exp=");
    assertThat(accessTokenExp).contains("access_token_exp=;");
    assertThat(accessTokenExp).doesNotContainIgnoringCase("HttpOnly");
    assertThat(accessTokenExp).contains("Path=/");
    assertThat(accessTokenExp).contains("SameSite=Strict");
    assertThat(accessTokenExp).contains("Max-Age=0");
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
