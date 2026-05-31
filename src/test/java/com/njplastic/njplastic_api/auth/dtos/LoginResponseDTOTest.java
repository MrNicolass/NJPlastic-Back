package com.njplastic.njplastic_api.auth.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.auth.enums.UserRole;

class LoginResponseDTOTest {

  private UserSummaryDTO summary() {
    return UserSummaryDTO.builder()
        .id(UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c"))
        .login("manager")
        .name("Manager Default")
        .role(UserRole.MANAGER)
        .build();
  }

  @Test
  void toString_masksTokenAndKeepsScheme() {
    LoginResponseDTO dto = LoginResponseDTO.builder()
        .token("super-secret-token")
        .tokenType("Bearer")
        .expiresInSeconds(3600L)
        .user(summary())
        .build();
    String text = dto.toString();
    assertThat(text).contains("token=***");
    assertThat(text).doesNotContain("super-secret-token");
    assertThat(text).contains("tokenType=Bearer");
    assertThat(text).contains("expiresInSeconds=3600");
  }

  @Test
  void builderAndAccessors_populateFields() {
    UserSummaryDTO user = summary();
    LoginResponseDTO dto = LoginResponseDTO.builder()
        .token("jwt")
        .tokenType("Bearer")
        .expiresInSeconds(28800L)
        .user(user)
        .build();
    assertThat(dto.getToken()).isEqualTo("jwt");
    assertThat(dto.getTokenType()).isEqualTo("Bearer");
    assertThat(dto.getExpiresInSeconds()).isEqualTo(28800L);
    assertThat(dto.getUser()).isSameAs(user);
  }

  @Test
  void settersUpdateFields() {
    LoginResponseDTO dto = new LoginResponseDTO();
    dto.setToken("t");
    dto.setTokenType("Bearer");
    dto.setExpiresInSeconds(10L);
    dto.setUser(summary());
    assertThat(dto.getToken()).isEqualTo("t");
    assertThat(dto.getExpiresInSeconds()).isEqualTo(10L);
    assertThat(dto.getUser()).isNotNull();
  }
}
