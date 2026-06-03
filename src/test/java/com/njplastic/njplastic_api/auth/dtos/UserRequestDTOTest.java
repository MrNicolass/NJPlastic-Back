package com.njplastic.njplastic_api.auth.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.auth.enums.UserRole;

class UserRequestDTOTest {

  @Test
  void toString_redactsPasswordAndKeepsOtherFields() {
    UserRequestDTO dto = UserRequestDTO.builder()
        .login("manager")
        .name("Manager Default")
        .email("manager@njplastic.com")
        .password("manager-dev-123")
        .role(UserRole.MANAGER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .build();

    String text = dto.toString();

    assertThat(text).contains("login=manager");
    assertThat(text).contains("name=Manager Default");
    assertThat(text).contains("email=manager@njplastic.com");
    assertThat(text).contains("password=***");
    assertThat(text).contains("role=MANAGER");
    assertThat(text).contains("sector=INJECAO");
    assertThat(text).contains("shift=TURNO_A");
    assertThat(text).doesNotContain("manager-dev-123");
  }
}
