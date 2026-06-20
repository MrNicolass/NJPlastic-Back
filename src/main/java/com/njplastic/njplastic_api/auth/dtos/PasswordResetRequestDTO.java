package com.njplastic.njplastic_api.auth.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload submitted to {@code POST /auth/password-reset} to start the recovery
 * flow. The endpoint always responds 204 regardless of whether the login
 * exists, so no enumeration leak is possible (OWASP A07).
 */
@Schema(description = "Password reset request payload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetRequestDTO {

  @Schema(description = "Login identifier", example = "manager", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  private String login;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("PasswordResetRequestDTO{login=").append(login)
        .append('}');
    return sb.toString();
  }
}
