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
 * Create payload for {@code POST /users}. The password is plain text on the
 * wire and BCrypt-hashed by the service before persistence. Sensitive fields
 * are marked {@code WRITE_ONLY} so Swagger doesn't echo them on the schema
 * preview.
 */
@Schema(description = "User creation payload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDTO {

  @Schema(description = "Login identifier", example = "manager", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(min = 3, max = 64)
  private String login;

  @Schema(description = "Display name", example = "Manager Default", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(max = 128)
  private String name;

  @Schema(description = "Contact email", example = "manager@njplastic.com", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Email
  @Size(max = 128)
  private String email;

  @Schema(description = "Plain-text password (>= 12 characters)", example = "manager-dev-123", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(min = 12)
  private String password;

  @Schema(description = "Authorization profile", example = "MANAGER", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  private UserRole role;

  @Schema(description = "Sector the user is bound to", example = "INJECAO", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  @Size(max = 64)
  private String sector;

  @Schema(description = "Shift the user is bound to", example = "TURNO_A", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  @Size(max = 32)
  private String shift;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("UserRequestDTO{login=").append(login)
        .append(", name=").append(name)
        .append(", email=").append(email)
        .append(", password=***")
        .append(", role=").append(role)
        .append(", sector=").append(sector)
        .append(", shift=").append(shift)
        .append('}');
    return sb.toString();
  }
}
