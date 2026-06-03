package com.njplastic.njplastic_api.auth.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordResetRequestDTOTest {

  @Test
  void toString_includesLogin() {
    PasswordResetRequestDTO dto = PasswordResetRequestDTO.builder().login("manager").build();

    assertThat(dto.toString()).contains("login=manager");
  }
}
