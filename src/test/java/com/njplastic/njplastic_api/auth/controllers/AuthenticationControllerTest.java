package com.njplastic.njplastic_api.auth.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njplastic.njplastic_api.auth.dtos.LoginRequestDTO;
import com.njplastic.njplastic_api.auth.dtos.LoginResponseDTO;
import com.njplastic.njplastic_api.auth.dtos.PasswordResetConfirmDTO;
import com.njplastic.njplastic_api.auth.dtos.PasswordResetRequestDTO;
import com.njplastic.njplastic_api.auth.dtos.UserSummaryDTO;
import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.exceptions.ExpiredResetTokenException;
import com.njplastic.njplastic_api.auth.exceptions.InvalidCredentialsException;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.auth.security.JwtTokenProvider;
import com.njplastic.njplastic_api.auth.services.AuthenticationService;
import com.njplastic.njplastic_api.auth.services.PasswordResetService;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

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

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void login_returns200WithToken() throws Exception {
    LoginResponseDTO response = LoginResponseDTO.builder()
        .token("jwt-token")
        .tokenType("Bearer")
        .expiresInSeconds(3600L)
        .user(UserSummaryDTO.builder()
            .id(UUID.randomUUID())
            .login("manager")
            .name("Manager Default")
            .role(UserRole.MANAGER)
            .build())
        .build();
    when(authenticationService.authenticate(any())).thenReturn(response);

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

  @BeforeEach
  void setUpPrincipal() {
    AuthenticatedUser principal = new AuthenticatedUser(
        UUID.randomUUID(), "manager", UserRole.MANAGER, "INJECAO", "TURNO_A");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
  }

  @Test
  void refresh_returnsFreshTokenAndExpiration() throws Exception {
    when(tokenProvider.refresh(any(AuthenticatedUser.class))).thenReturn("refreshed-token");
    when(tokenProvider.expirationSeconds()).thenReturn(3600L);

    mockMvc.perform(post("/auth/refresh"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("refreshed-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresInSeconds").value(3600));
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
}
