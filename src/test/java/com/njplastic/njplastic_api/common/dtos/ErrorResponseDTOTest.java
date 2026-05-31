package com.njplastic.njplastic_api.common.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorResponseDTOTest {

  @Test
  void builder_populatesAllFields() {
    ErrorResponseDTO dto = ErrorResponseDTO.builder()
        .message("Resource not found")
        .description("The requested resource was missing")
        .clazzError("ResourceNotFoundException")
        .build();

    assertThat(dto.getMessage()).isEqualTo("Resource not found");
    assertThat(dto.getDescription()).isEqualTo("The requested resource was missing");
    assertThat(dto.getClazzError()).isEqualTo("ResourceNotFoundException");
    assertThat(dto.getTimestamp()).isNotNull();
  }

  @Test
  void builder_setsTimestampOnBuild() {
    ErrorResponseDTO before = ErrorResponseDTO.builder().message("x").clazzError("E").build();
    ErrorResponseDTO after = ErrorResponseDTO.builder().message("x").clazzError("E").build();
    assertThat(before.getTimestamp()).isNotNull();
    assertThat(after.getTimestamp()).isNotNull();
    assertThat(after.getTimestamp()).isAfterOrEqualTo(before.getTimestamp());
  }

  @Test
  void toString_includesAllFields() {
    ErrorResponseDTO dto = ErrorResponseDTO.builder()
        .message("msg")
        .description("desc")
        .clazzError("Boom")
        .build();
    String text = dto.toString();
    assertThat(text)
        .contains("ErrorResponseDTO{")
        .contains("message=msg")
        .contains("description=desc")
        .contains("clazzError=Boom");
  }

  @Test
  void builder_allowsNullOptionalFields() {
    ErrorResponseDTO dto = ErrorResponseDTO.builder()
        .message("only required")
        .clazzError("Boom")
        .build();
    assertThat(dto.getDescription()).isNull();
    assertThat(dto.getMessage()).isEqualTo("only required");
    assertThat(dto.getClazzError()).isEqualTo("Boom");
  }
}
