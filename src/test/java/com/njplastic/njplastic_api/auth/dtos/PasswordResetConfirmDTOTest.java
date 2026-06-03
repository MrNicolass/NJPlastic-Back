package com.njplastic.njplastic_api.auth.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordResetConfirmDTOTest {

  @Test
  void toString_redactsSensitiveFields() {
    PasswordResetConfirmDTO dto = PasswordResetConfirmDTO.builder()
        .token("opaque-token-secret")
        .newPassword("very-secret-password")
        .build();

    String text = dto.toString();

    assertThat(text).contains("token=***");
    assertThat(text).contains("newPassword=***");
    assertThat(text).doesNotContain("opaque-token-secret");
    assertThat(text).doesNotContain("very-secret-password");
  }
}
