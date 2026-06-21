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
 * Credentials submitted to {@code POST /auth/login}. The 12-character minimum
 * on the password is enforced both here (input-side) and at user creation
 * time.
 */
@Schema(description = "Credentials payload for authentication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

  @Schema(description = "Login identifier", example = "manager", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  private String login;

  @Schema(description = "Plain-text password (>= 12 characters)", example = "manager-dev-123", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(min = 12)
  private String password;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("LoginRequestDTO{login=").append(login)
        .append(", password=***")
        .append('}');
    return sb.toString();
  }
}
