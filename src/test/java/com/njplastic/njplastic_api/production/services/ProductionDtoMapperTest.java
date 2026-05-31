package com.njplastic.njplastic_api.production.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.production.dtos.MachineStatusEntryDTO;
import com.njplastic.njplastic_api.production.dtos.MachineSummaryDTO;
import com.njplastic.njplastic_api.production.dtos.ProductionCycleResponseDTO;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.entities.ProductionCycle;
import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecordState;

class ProductionDtoMapperTest {

  private final ProductionDtoMapper mapper = new ProductionDtoMapper();

  private Machine machine() {
    return Machine.builder()
        .id(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .code("MAQ-01")
        .description("Injection molder")
        .sector("INJECAO")
        .standardCycleMs(2000)
        .consecutivePausesToStop(3)
        .active(true)
        .build();
  }

  private MachineStatus status() {
    return MachineStatus.builder()
        .id(UUID.fromString("7c8d9e0f-1a2b-3c4d-5e6f-7a8b9c0d1e2f"))
        .machineId(machine().getId())
        .state(MachineState.AUTO_STOPPED)
        .reason("TROCA_DE_MOLDE")
        .message("Stopped after 3 pauses")
        .startTime(OffsetDateTime.parse("2026-05-28T14:25:00Z"))
        .endTime(OffsetDateTime.parse("2026-05-28T14:40:00Z"))
        .reasonAuthorId(UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c"))
        .consecutiveCountAtCreation(3)
        .recordState(RecordState.CONFIRMED)
        .build();
  }

  private ProductionCycle cycle() {
    return ProductionCycle.builder()
        .id(UUID.fromString("1b2c3d4e-5f6a-7b8c-9d0e-1f2a3b4c5d6e"))
        .machineId(machine().getId())
        .pulseTimestamp(OffsetDateTime.parse("2026-05-28T14:23:55Z"))
        .receivedAt(OffsetDateTime.parse("2026-05-28T14:23:55Z"))
        .sequence(42L)
        .intervalMs(2010)
        .state(RecordState.CONFIRMED)
        .build();
  }

  @Test
  void toMachineSummary_copiesFieldsAndCurrentState() {
    MachineSummaryDTO dto = mapper.toMachineSummary(machine(), MachineState.RUNNING);
    assertThat(dto.getId()).isEqualTo(machine().getId());
    assertThat(dto.getCode()).isEqualTo("MAQ-01");
    assertThat(dto.getSector()).isEqualTo("INJECAO");
    assertThat(dto.getStandardCycleMs()).isEqualTo(2000);
    assertThat(dto.getConsecutivePausesToStop()).isEqualTo(3);
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.getCurrentState()).isEqualTo(MachineState.RUNNING);
  }

  @Test
  void toMachineSummary_acceptsNullCurrentState() {
    MachineSummaryDTO dto = mapper.toMachineSummary(machine(), null);
    assertThat(dto.getCurrentState()).isNull();
  }

  @Test
  void toStatusEntry_copiesNonAuditFields() {
    MachineStatusEntryDTO dto = mapper.toStatusEntry(status());
    assertThat(dto.getId()).isEqualTo(status().getId());
    assertThat(dto.getState()).isEqualTo(MachineState.AUTO_STOPPED);
    assertThat(dto.getReason()).isEqualTo("TROCA_DE_MOLDE");
    assertThat(dto.getMessage()).isEqualTo("Stopped after 3 pauses");
    assertThat(dto.getStartTime()).isEqualTo(OffsetDateTime.parse("2026-05-28T14:25:00Z"));
    assertThat(dto.getEndTime()).isEqualTo(OffsetDateTime.parse("2026-05-28T14:40:00Z"));
    assertThat(dto.getReasonAuthorId()).isEqualTo(status().getReasonAuthorId());
    assertThat(dto.getConsecutiveCountAtCreation()).isEqualTo(3);
  }

  @Test
  void toStatusEntries_mapsEveryRecord() {
    List<MachineStatusEntryDTO> entries = mapper.toStatusEntries(List.of(status(), status()));
    assertThat(entries).hasSize(2);
    assertThat(entries.get(0).getId()).isEqualTo(status().getId());
  }

  @Test
  void toStatusEntries_emptyInputYieldsEmptyOutput() {
    assertThat(mapper.toStatusEntries(List.of())).isEmpty();
  }

  @Test
  void toCycleResponse_copiesAllFields() {
    ProductionCycleResponseDTO dto = mapper.toCycleResponse(cycle());
    assertThat(dto.getId()).isEqualTo(cycle().getId());
    assertThat(dto.getMachineId()).isEqualTo(cycle().getMachineId());
    assertThat(dto.getPulseTimestamp()).isEqualTo(cycle().getPulseTimestamp());
    assertThat(dto.getReceivedAt()).isEqualTo(cycle().getReceivedAt());
    assertThat(dto.getSequence()).isEqualTo(42L);
    assertThat(dto.getIntervalMs()).isEqualTo(2010);
    assertThat(dto.getState()).isEqualTo(RecordState.CONFIRMED);
  }
}
