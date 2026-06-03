package com.njplastic.njplastic_api.auth.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshResponseDTOTest {

  @Test
  void toString_redactsToken() {
    RefreshResponseDTO dto = RefreshResponseDTO.builder()
        .token("eyJhbGciOiJIUzI1NiJ9.payload.signature")
        .tokenType("Bearer")
        .expiresInSeconds(28800L)
        .build();

    String text = dto.toString();

    assertThat(text).contains("token=***");
    assertThat(text).contains("tokenType=Bearer");
    assertThat(text).contains("expiresInSeconds=28800");
    assertThat(text).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
  }
}
