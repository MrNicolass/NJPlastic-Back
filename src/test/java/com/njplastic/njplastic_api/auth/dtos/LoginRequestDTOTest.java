package com.njplastic.njplastic_api.auth.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginRequestDTOTest {

  @Test
  void toString_masksPasswordAndKeepsLogin() {
    LoginRequestDTO dto = LoginRequestDTO.builder()
        .login("manager")
        .password("manager-dev-123")
        .build();
    String text = dto.toString();
    assertThat(text).contains("login=manager");
    assertThat(text).contains("password=***");
    assertThat(text).doesNotContain("manager-dev-123");
  }

  @Test
  void builderAndAccessors_populateFields() {
    LoginRequestDTO dto = LoginRequestDTO.builder()
        .login("operator")
        .password("a-very-strong-password")
        .build();
    assertThat(dto.getLogin()).isEqualTo("operator");
    assertThat(dto.getPassword()).isEqualTo("a-very-strong-password");
  }

  @Test
  void settersUpdateFields() {
    LoginRequestDTO dto = new LoginRequestDTO();
    dto.setLogin("leader");
    dto.setPassword("twelve-chars");
    assertThat(dto.getLogin()).isEqualTo("leader");
    assertThat(dto.getPassword()).isEqualTo("twelve-chars");
  }
}
