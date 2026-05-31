package com.njplastic.njplastic_api.auth.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload for a successful authentication at {@code POST /auth/login}.
 * Carries the issued JWT and a non-sensitive summary of the user.
 */
@Schema(description = "Authentication response containing the issued JWT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

  @Schema(description = "Compact JWT token (HS256)", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzZjFjMmI5ZSJ9.sig", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String token;

  @Schema(description = "Token scheme to use on the Authorization header", example = "Bearer", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String tokenType;

  @Schema(description = "Lifetime of the token in seconds", example = "28800", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private long expiresInSeconds;

  @Schema(description = "Authenticated user summary", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UserSummaryDTO user;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("LoginResponseDTO{token=***")
        .append(", tokenType=").append(tokenType)
        .append(", expiresInSeconds=").append(expiresInSeconds)
        .append(", user=").append(user)
        .append('}');
    return sb.toString();
  }
}
