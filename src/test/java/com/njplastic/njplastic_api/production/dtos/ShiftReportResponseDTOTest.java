package com.njplastic.njplastic_api.production.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.production.dtos.ShiftReportResponseDTO.MachineReportSection;

class ShiftReportResponseDTOTest {

  private MachineSummaryDTO machine() {
    return MachineSummaryDTO.builder()
        .id(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .code("MAQ-01")
        .standardCycleMs(2000)
        .consecutivePausesToStop(3)
        .active(true)
        .build();
  }

  private MachineReportSection section() {
    return MachineReportSection.builder()
        .machine(machine())
        .confirmedCycles(1280L)
        .oee(OeeResultDTO.builder()
            .machineId(machine().getId())
            .periodStart(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
            .periodEnd(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
            .availability(0.9)
            .performance(0.8)
            .partial(true)
            .build())
        .manualPauses(List.of())
        .autoStops(List.of())
        .build();
  }

  @Test
  void builder_populatesAllFields() {
    ShiftReportResponseDTO dto = ShiftReportResponseDTO.builder()
        .periodStart(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
        .periodEnd(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .sector("INJECAO")
        .shift("TURNO_A")
        .machines(List.of(section()))
        .build();

    assertThat(dto.getSector()).isEqualTo("INJECAO");
    assertThat(dto.getShift()).isEqualTo("TURNO_A");
    assertThat(dto.getMachines()).hasSize(1);
  }

  @Test
  void toString_usesMachinesSize() {
    ShiftReportResponseDTO dto = ShiftReportResponseDTO.builder()
        .periodStart(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
        .periodEnd(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .machines(List.of(section()))
        .build();
    assertThat(dto.toString())
        .contains("ShiftReportResponseDTO{")
        .contains("machinesSize=1");
  }

  @Test
  void toString_handlesNullCollections() {
    ShiftReportResponseDTO dto = new ShiftReportResponseDTO();
    assertThat(dto.toString()).contains("machinesSize=0");
  }

  @Test
  void machineReportSection_builderPopulatesAndToStringIncludesCounts() {
    MachineReportSection s = section();
    assertThat(s.getConfirmedCycles()).isEqualTo(1280L);
    assertThat(s.getMachine().getCode()).isEqualTo("MAQ-01");
    assertThat(s.toString())
        .contains("MachineReportSection{")
        .contains("confirmedCycles=1280")
        .contains("manualPausesSize=0")
        .contains("autoStopsSize=0");
  }

  @Test
  void machineReportSection_toStringHandlesNullCollections() {
    MachineReportSection s = new MachineReportSection();
    assertThat(s.toString())
        .contains("manualPausesSize=0")
        .contains("autoStopsSize=0");
  }
}
