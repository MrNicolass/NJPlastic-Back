package com.njplastic.njplastic_api.erp.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Read-only projection of one ErpSyncRun entry, exposed in the recent-runs
 * list of {@code GET /erp/sync/status} (UC08, Figura 26b). Mirrors
 * {@link com.njplastic.njplastic_api.erp.entities.ErpSyncRun} 1:1 with the
 * sole exception that {@code errorMessage} is omitted when the run finished
 * SUCCESS to keep the payload terse.
 */
@Schema(description = "ERP sync execution entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpSyncRunDTO {

  @Schema(description = "Sync run UUID", example = "1b2c3d4e-5f6a-7b8c-9d0e-1f2a3b4c5d6e", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private UUID id;

  @Schema(description = "Instant the sync started", example = "2026-05-28T14:00:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime startedAt;

  @Schema(description = "Instant the sync finished; null while RUNNING", example = "2026-05-28T14:00:03Z", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private OffsetDateTime finishedAt;

  @Schema(description = "Outcome of the sync execution", example = "SUCCESS", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private ErpSyncStatus status;

  @Schema(description = "Number of open orders read from the ERP", example = "12", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer ordersRead;

  @Schema(description = "Number of production cycles written to the ERP", example = "240", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer cyclesWritten;

  @Schema(description = "Number of pause/auto-stop records written to the ERP", example = "5", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer pausesWritten;

  @Schema(description = "Wall-clock duration of the sync in milliseconds", example = "1840", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private Integer durationMs;

  @Schema(description = "Error message captured when the sync failed; omitted on SUCCESS", example = "ERP connection timeout", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String errorMessage;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ErpSyncRunDTO{id=").append(id)
        .append(", startedAt=").append(startedAt)
        .append(", finishedAt=").append(finishedAt)
        .append(", status=").append(status)
        .append(", ordersRead=").append(ordersRead)
        .append(", cyclesWritten=").append(cyclesWritten)
        .append(", pausesWritten=").append(pausesWritten)
        .append(", durationMs=").append(durationMs)
        .append('}');
    return sb.toString();
  }
}