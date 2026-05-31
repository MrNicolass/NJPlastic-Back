package com.njplastic.njplastic_api.production.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EditStopMessageRequestDTOTest {

  @Test
  void builderAndAccessors_populateField() {
    EditStopMessageRequestDTO dto = EditStopMessageRequestDTO.builder()
        .message("Mold change - batch 4321")
        .build();
    assertThat(dto.getMessage()).isEqualTo("Mold change - batch 4321");
  }

  @Test
  void toString_includesMessage() {
    EditStopMessageRequestDTO dto = EditStopMessageRequestDTO.builder().message("Stopped").build();
    assertThat(dto.toString())
        .contains("EditStopMessageRequestDTO{")
        .contains("message=Stopped");
  }

  @Test
  void settersUpdateField() {
    EditStopMessageRequestDTO dto = new EditStopMessageRequestDTO();
    dto.setMessage("new");
    assertThat(dto.getMessage()).isEqualTo("new");
  }
}
