package com.njplastic.njplastic_api.auth.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njplastic.njplastic_api.auth.dtos.LoginRequest;
import com.njplastic.njplastic_api.auth.dtos.LoginResponse;
import com.njplastic.njplastic_api.auth.dtos.UserSummary;
import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.exceptions.InvalidCredentialsException;
import com.njplastic.njplastic_api.auth.services.AuthenticationService;

@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean
  private AuthenticationService authenticationService;

  private String json(String login, String password) throws Exception {
    return objectMapper.writeValueAsString(
        LoginRequest.builder().login(login).password(password).build());
  }

  @Test
  void login_returns200WithToken() throws Exception {
    LoginResponse response = LoginResponse.builder()
        .token("jwt-token")
        .tokenType("Bearer")
        .expiresInSeconds(3600L)
        .user(UserSummary.builder()
            .id(UUID.randomUUID())
            .login("manager")
            .name("Manager Default")
            .role(UserRole.MANAGER)
            .build())
        .build();
    when(authenticationService.authenticate(any())).thenReturn(response);

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("manager", "manager-dev-123")))
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
            .content(json("manager", "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Credenciais inválidas"))
        .andExpect(jsonPath("$.clazzError").value("InvalidCredentialsException"));
  }

  @Test
  void login_returns400OnInvalidPayload() throws Exception {
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("", "short")))
        .andExpect(status().isBadRequest());
  }
}
