package com.njplastic.njplastic_api.production.dtos;

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
 * Body of {@code PUT /machines/{id}/stops/{stopId}/message}. Replaces
 * the editable message of an AUTO_STOPPED record (RF18, RF19, UC12). The
 * audit trail is written by the global {@code AuditFilter} (RF20, RN12).
 */
@Schema(description = "New message text for an AUTO_STOPPED record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditStopMessageRequestDTO {

  @Schema(description = "Replacement message displayed on dashboards and reports", example = "Mold change - batch 4321", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(max = 2000)
  private String message;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("EditStopMessageRequestDTO{message=").append(message)
        .append('}');
    return sb.toString();
  }
}
