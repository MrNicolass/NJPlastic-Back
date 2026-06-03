package com.njplastic.njplastic_api.auth.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin-side projection of a {@link User}. Carries every field the Manager
 * needs in the {@code Users_Part1/2_V1} mockup screens, but never the
 * password hash.
 */
@Schema(description = "Admin projection of a user (no password hash)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

  @Schema(description = "User UUID", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Login identifier", example = "manager", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String login;

  @Schema(description = "Display name", example = "Manager Default", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String name;

  @Schema(description = "Contact email", example = "manager@njplastic.com", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String email;

  @Schema(description = "Authorization profile", example = "MANAGER", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UserRole role;

  @Schema(description = "Sector the user is bound to", example = "INJECAO", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String sector;

  @Schema(description = "Shift the user is bound to", example = "TURNO_A", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String shift;

  @Schema(description = "Whether the account is active", example = "true", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private boolean active;

  @Schema(description = "Creation timestamp", example = "2026-05-28T08:30:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime createdAt;

  @Schema(description = "Last update timestamp", example = "2026-05-28T08:30:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime updatedAt;

  /**
   * Build a response DTO from a persisted entity.
   *
   * @param user source entity
   * @return populated DTO
   */
  public static UserResponseDTO from(User user) {
    return UserResponseDTO.builder()
        .id(user.getId())
        .login(user.getLogin())
        .name(user.getName())
        .email(user.getEmail())
        .role(user.getRole())
        .sector(user.getSector())
        .shift(user.getShift())
        .active(user.isActive())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("UserResponseDTO{id=").append(id)
        .append(", login=").append(login)
        .append(", name=").append(name)
        .append(", email=").append(email)
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
