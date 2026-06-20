package com.njplastic.njplastic_api.auth.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

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
 * Public-safe view of a {@link User}. Never exposes the password hash.
 */
@Schema(description = "Non-sensitive projection of a user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDTO {

  @Schema(description = "User UUID", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Login identifier", example = "manager", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String login;

  @Schema(description = "Display name", example = "Manager Default", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String name;

  @Schema(description = "Authorization profile", example = "MANAGER", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UserRole role;

  @Schema(description = "Sector the user is bound to", example = "INJECAO", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String sector;

  @Schema(description = "Shift the user is bound to", example = "TURNO_A", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String shift;

 /**
 * Build a UserSummaryDTO from a persisted {@link User} entity.
 *
 * @param user the source entity
 * @return a populated UserSummaryDTO instance
 */
  public static UserSummaryDTO from(User user) {
    return UserSummaryDTO.builder()
        .id(user.getId())
        .login(user.getLogin())
        .name(user.getName())
        .role(user.getRole())
        .sector(user.getSector())
        .shift(user.getShift())
        .build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("UserSummaryDTO{id=").append(id)
        .append(", login=").append(login)
        .append(", name=").append(name)
        .append(", role=").append(role)
        .append(", sector=").append(sector)
        .append(", shift=").append(shift)
        .append('}');
    return sb.toString();
  }
}
