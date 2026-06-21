package com.njplastic.njplastic_api.auth.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.UUID;

import com.njplastic.njplastic_api.auth.entities.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal projection of an active operator scoped to a machine's sector
 * and shift. Returned by {@code GET /machines/{id}/operators} to feed the
 * "Operators of shift" card on the machine detail screen.
 */
@Schema(description = "Active operator assigned to a machine for the current shift")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorOfShiftDTO {

  @Schema(description = "User UUID", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Display name", example = "Operadora A", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String name;

  @Schema(description = "Shift the operator is bound to", example = "TURNO_A", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String shift;

 /**
 * Build an OperatorOfShiftDTO from a persisted {@link User} entity.
 *
 * @param user the source entity
 * @return a populated OperatorOfShiftDTO instance
 */
  public static OperatorOfShiftDTO from(User user) {
    return OperatorOfShiftDTO.builder()
        .id(user.getId())
        .name(user.getName())
        .shift(user.getShift())
        .build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("OperatorOfShiftDTO{id=").append(id)
        .append(", name=").append(name)
        .append(", shift=").append(shift)
        .append('}');
    return sb.toString();
  }
}
