package com.njplastic.njplastic_api.auth.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.njplastic.njplastic_api.auth.enums.UserRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Update payload for {@code PUT /users/{id}}. Deliberately omits {@code login}
 * (immutable - identifies the account in the audit trail) and {@code password}
 * (rotated only via the password-reset flow). {@code active} stays a separate
 * field to support soft-reactivation by the admin without going through
 * {@code DELETE}.
 */
@Schema(description = "User update payload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequestDTO {

  @Schema(description = "Display name", example = "Manager Default", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(max = 128)
  private String name;

  @Schema(description = "Contact email", example = "manager@njplastic.com", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Email
  @Size(max = 128)
  private String email;

  @Schema(description = "Authorization profile", example = "MANAGER", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  private UserRole role;

  @Schema(description = "Sector the user is bound to", example = "INJECAO", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  @Size(max = 64)
  private String sector;

  @Schema(description = "Shift the user is bound to", example = "TURNO_A", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  @Size(max = 32)
  private String shift;

  @Schema(description = "Whether the account is active", example = "true", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  private boolean active;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("UserUpdateRequestDTO{name=").append(name)
        .append(", email=").append(email)
        .append(", role=").append(role)
        .append(", sector=").append(sector)
        .append(", shift=").append(shift)
        .append(", active=").append(active)
        .append('}');
    return sb.toString();
  }
}
