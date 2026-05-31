package com.njplastic.njplastic_api.production.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegisterPauseRequestDTOTest {

  @Test
  void builderAndAccessors_populateField() {
    RegisterPauseRequestDTO dto = RegisterPauseRequestDTO.builder().reason("Mold change").build();
    assertThat(dto.getReason()).isEqualTo("Mold change");
  }

  @Test
  void toString_includesReason() {
    RegisterPauseRequestDTO dto = RegisterPauseRequestDTO.builder().reason("Setup").build();
    assertThat(dto.toString())
        .contains("RegisterPauseRequestDTO{")
        .contains("reason=Setup");
  }

  @Test
  void settersUpdateField() {
    RegisterPauseRequestDTO dto = new RegisterPauseRequestDTO();
    dto.setReason("other");
    assertThat(dto.getReason()).isEqualTo("other");
  }
}
