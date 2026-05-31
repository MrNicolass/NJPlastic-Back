package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Consolidated shift report (RF15). Aggregates, for every machine in the
 * caller scope, the OEE result for the window plus the list of manual
 * pauses and auto stops with their messages and authors. CSV/PDF export
 * is delivered separately by RF16/EP-FE-07; this endpoint returns JSON
 * only.
 */
@Schema(description = "Consolidated shift report for a sector/shift window (RF15)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftReportResponseDTO {

  @Schema(description = "Start of the evaluated window", example = "2026-05-28T06:00:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime periodStart;

  @Schema(description = "End of the evaluated window", example = "2026-05-28T14:00:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime periodEnd;

  @Schema(description = "Sector filter applied; null when the caller scope already constrains it", example = "INJECAO", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String sector;

  @Schema(description = "Shift label applied to the time window; informational only", example = "TURNO_A", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String shift;

  @Schema(description = "Per-machine sections aggregated by the report", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private List<MachineReportSection> machines;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ShiftReportResponseDTO{periodStart=").append(periodStart)
        .append(", periodEnd=").append(periodEnd)
        .append(", sector=").append(sector)
        .append(", shift=").append(shift)
        .append(", machinesSize=").append(machines == null ? 0 : machines.size())
        .append('}');
    return sb.toString();
  }

  /**
   * Per-machine section of the shift report: identification, totals, OEE
   * for the window and the lists of manual pauses and auto stops.
   */
  @Schema(description = "Per-machine section of the shift report")
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MachineReportSection {

    @Schema(description = "Machine summary", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
    private MachineSummaryDTO machine;

    @Schema(description = "Number of confirmed cycles in the window", example = "1280", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
    private long confirmedCycles;

    @Schema(description = "OEE result for the window (RF10)", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
    private OeeResultDTO oee;

    @Schema(description = "Manual pauses classified by users (type=PAUSED)", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
    private List<MachineStatusEntryDTO> manualPauses;

    @Schema(description = "Auto stops detected by the system (type=AUTO_STOPPED) with current message and author (RF20)", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
    private List<MachineStatusEntryDTO> autoStops;

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("MachineReportSection{machine=").append(machine)
          .append(", confirmedCycles=").append(confirmedCycles)
          .append(", oee=").append(oee)
          .append(", manualPausesSize=").append(manualPauses == null ? 0 : manualPauses.size())
          .append(", autoStopsSize=").append(autoStops == null ? 0 : autoStops.size())
          .append('}');
      return sb.toString();
    }
  }
}
