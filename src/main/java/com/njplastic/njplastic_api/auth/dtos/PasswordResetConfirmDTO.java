package com.njplastic.njplastic_api.auth.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload submitted to {@code POST /auth/password-reset/confirm} to finish the
 * recovery flow. The opaque token comes from the recovery email; the new
 * password is BCrypt-hashed on the server before persistence.
 */
@Schema(description = "Password reset confirmation payload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetConfirmDTO {

  @Schema(description = "Opaque token received by email", example = "Y4tWQk8b...zZ9c", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  private String token;

  @Schema(description = "New plain-text password (>= 12 characters)", example = "new-secure-password-123", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(min = 12)
  private String newPassword;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("PasswordResetConfirmDTO{token=***")
        .append(", newPassword=***")
        .append('}');
    return sb.toString();
  }
}
