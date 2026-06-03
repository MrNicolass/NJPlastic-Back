package com.njplastic.njplastic_api.auth.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PasswordResetTokenTest {

  @Test
  void onCreate_generatesIdAndCreatedAtWhenMissing() {
    PasswordResetToken token = PasswordResetToken.builder()
        .userId(UUID.randomUUID())
        .token("opaque")
        .expiresAt(OffsetDateTime.parse("2026-06-01T10:30:00Z"))
        .build();

    token.onCreate();

    assertThat(token.getId()).isNotNull();
    assertThat(token.getCreatedAt()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingIdAndCreatedAt() {
    UUID id = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-01T10:00:00Z");
    PasswordResetToken token = PasswordResetToken.builder()
        .id(id)
        .createdAt(createdAt)
        .userId(UUID.randomUUID())
        .token("opaque")
        .expiresAt(OffsetDateTime.parse("2026-06-01T10:30:00Z"))
        .build();

    token.onCreate();

    assertThat(token.getId()).isEqualTo(id);
    assertThat(token.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  void toString_redactsToken() {
    PasswordResetToken token = PasswordResetToken.builder()
        .id(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .token("super-secret-token")
        .expiresAt(OffsetDateTime.parse("2026-06-01T10:30:00Z"))
        .build();

    assertThat(token.toString()).contains("token=***");
    assertThat(token.toString()).doesNotContain("super-secret-token");
  }
}
