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
 * Body of {@code POST /machines/{id}/pauses}. Carries the reason the
 * operator selected for the latest isolated pause detected on a machine
 * (RF09, UC03). The endpoint resolves the target record itself; the
 * client only needs to supply the classification text.
 */
@Schema(description = "Reason supplied to classify the latest open isolated pause")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterPauseRequestDTO {

  @Schema(description = "Reason text registered for the pause", example = "Mold change", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(max = 255)
  private String reason;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("RegisterPauseRequestDTO{reason=").append(reason)
        .append('}');
    return sb.toString();
  }
}
