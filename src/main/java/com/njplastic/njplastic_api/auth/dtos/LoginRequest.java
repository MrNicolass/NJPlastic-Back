package com.njplastic.njplastic_api.auth.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Credentials submitted to {@code POST /auth/login}. The 12-character minimum
 * on the password is enforced both here (input-side) and at user creation
 * time, aligned with RFC §6.2.
 */
@Schema(description = "Credentials payload for authentication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

  @Schema(description = "Login identifier", example = "manager", requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.WRITE_ONLY, nullable = false)
  @NotBlank
  private String login;

  @Schema(description = "Plain-text password (>= 12 characters)", example = "manager-dev-123", requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(min = 12)
  private String password;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("LoginRequest{login=").append(login)
        .append(", password=***")
        .append('}');
    return sb.toString();
  }
}