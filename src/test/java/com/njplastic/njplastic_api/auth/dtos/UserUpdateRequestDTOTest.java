package com.njplastic.njplastic_api.auth.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.auth.enums.UserRole;

class UserUpdateRequestDTOTest {

  @Test
  void toString_carriesEveryField() {
    UserUpdateRequestDTO dto = UserUpdateRequestDTO.builder()
        .name("Manager Default")
        .email("manager@njplastic.com")
        .role(UserRole.MANAGER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .active(true)
        .build();

    String text = dto.toString();

    assertThat(text).contains("name=Manager Default");
    assertThat(text).contains("email=manager@njplastic.com");
    assertThat(text).contains("role=MANAGER");
    assertThat(text).contains("sector=INJECAO");
    assertThat(text).contains("shift=TURNO_A");
    assertThat(text).contains("active=true");
  }
}
