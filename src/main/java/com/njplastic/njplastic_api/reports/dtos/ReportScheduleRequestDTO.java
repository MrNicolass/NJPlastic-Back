package com.njplastic.njplastic_api.reports.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.njplastic.njplastic_api.reports.enums.ReportFormat;
import com.njplastic.njplastic_api.reports.enums.ReportType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload submitted to {@code POST /reports/schedule}. The author UUID is
 * resolved from the JWT, never from the payload.
 */
@Schema(description = "Report schedule create payload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportScheduleRequestDTO {

  @Schema(description = "Report category", example = "SHIFT", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  private ReportType type;

  @Schema(description = "Free-form JSON parameters consumed by the renderer", example = "{\"sector\":\"INJECAO\",\"shift\":\"TURNO_A\"}", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = true)
  private String params;

  @Schema(description = "Spring 6-field cron expression", example = "0 0 7 * * MON-FRI", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Size(max = 64)
  private String cron;

  @Schema(description = "Destination email", example = "manager@njplastic.com", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  @Email
  @Size(max = 255)
  private String deliveryEmail;

  @Schema(description = "Output format", example = "CSV", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  private ReportFormat format;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ReportScheduleRequestDTO{type=").append(type)
        .append(", cron=").append(cron)
        .append(", deliveryEmail=").append(deliveryEmail)
        .append(", format=").append(format)
        .append('}');
    return sb.toString();
  }
}
