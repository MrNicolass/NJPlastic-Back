package com.njplastic.njplastic_api.auth.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.njplastic.njplastic_api.auth.enums.UserRole;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Application user. Maps to the "users" table created by V1__init.sql.
 * Referential integrity for user_id elsewhere is enforced in the service
 * layer (see - "no REFERENCES in PostgreSQL").
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Schema(description = "User UUID", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", nullable = false)
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Schema(description = "Login identifier", example = "manager", nullable = false)
  @Column(name = "login", nullable = false, unique = true, length = 64)
  private String login;

  @Schema(description = "Display name", example = "Manager Default", nullable = false)
  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Schema(description = "Contact email", example = "manager@njplastic.com", nullable = false)
  @Column(name = "email", nullable = false, length = 128)
  private String email;

  @Schema(description = "BCrypt password hash (factor 12)", accessMode = READ_ONLY, nullable = false)
  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Schema(description = "Authorization profile", example = "MANAGER", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "role", nullable = false, columnDefinition = "user_role")
  private UserRole role;

  @Schema(description = "Sector the user is bound to", example = "INJECAO", nullable = true)
  @Column(name = "sector", length = 64)
  private String sector;

  @Schema(description = "Shift the user is bound to", example = "TURNO_A", nullable = true)
  @Column(name = "shift", length = 32)
  private String shift;

  @Schema(description = "Whether the account is active", example = "true", nullable = false)
  @Column(name = "active", nullable = false)
  private boolean active;

  @Schema(description = "Creation timestamp", example = "2026-05-28T08:30:00Z", nullable = false)
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Schema(description = "Last update timestamp", example = "2026-05-28T08:30:00Z", nullable = false)
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("User{id=").append(id)
        .append(", login=").append(login)
        .append(", name=").append(name)
        .append(", email=").append(email)
        .append(", passwordHash=***")
        .append(", role=").append(role)
        .append(", sector=").append(sector)
        .append(", shift=").append(shift)
        .append(", active=").append(active)
        .append(", createdAt=").append(createdAt)
        .append(", updatedAt=").append(updatedAt)
        .append('}');
    return sb.toString();
  }
}