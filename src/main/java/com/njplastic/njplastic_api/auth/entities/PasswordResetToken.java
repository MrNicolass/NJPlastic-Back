package com.njplastic.njplastic_api.auth.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Single-use password reset token (reopened). Maps to the
 * password_reset_token table created by V7__password_reset_token.sql. The
 * raw {@code token} is stored verbatim because TTL is short; consumed tokens
 * are marked via {@code usedAt} instead of being deleted, so audit can trace
 * the reset back to the request.
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

  @Schema(description = "Token UUID", example = "5a4b3c2d-1e0f-9a8b-7c6d-5e4f3a2b1c0d", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Owner user UUID", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", nullable = false)
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Schema(description = "Opaque random token shared by email", accessMode = READ_ONLY, nullable = false)
  @Column(name = "token", nullable = false, unique = true, length = 255, updatable = false)
  private String token;

  @Schema(description = "Expiration timestamp", example = "2026-06-01T10:30:00Z", nullable = false)
  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Schema(description = "When the token was consumed; null while pending", example = "2026-06-01T10:25:00Z", nullable = true)
  @Column(name = "used_at")
  private OffsetDateTime usedAt;

  @Schema(description = "Creation timestamp", example = "2026-06-01T10:00:00Z", nullable = false)
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("PasswordResetToken{id=").append(id)
        .append(", userId=").append(userId)
        .append(", token=***")
        .append(", expiresAt=").append(expiresAt)
        .append(", usedAt=").append(usedAt)
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
