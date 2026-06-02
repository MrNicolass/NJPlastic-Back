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
 * Response payload for {@code POST /auth/refresh}. Returns a fresh JWT with
 * the same claims as the previous one and a new {@code exp}. Mirrors the
 * top-level structure of {@link LoginResponseDTO} minus the user summary -
 * the frontend already has the user cached when calling refresh.
 */
@Schema(description = "Refreshed JWT response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshResponseDTO {

  @Schema(description = "Compact JWT token (HS256)", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzZjFjMmI5ZSJ9.sig", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String token;

  @Schema(description = "Token scheme to use on the Authorization header", example = "Bearer", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String tokenType;

  @Schema(description = "Lifetime of the new token in seconds", example = "28800", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private long expiresInSeconds;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("RefreshResponseDTO{token=***")
        .append(", tokenType=").append(tokenType)
        .append(", expiresInSeconds=").append(expiresInSeconds)
        .append('}');
    return sb.toString();
  }
}
