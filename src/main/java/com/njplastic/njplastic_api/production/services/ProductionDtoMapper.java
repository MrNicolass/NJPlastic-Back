package com.njplastic.njplastic_api.production.services;

import java.util.List;

import org.springframework.stereotype.Component;

import com.njplastic.njplastic_api.production.dtos.MachineStatusEntryDTO;
import com.njplastic.njplastic_api.production.dtos.MachineSummaryDTO;
import com.njplastic.njplastic_api.production.dtos.ProductionCycleResponseDTO;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.entities.ProductionCycle;
import com.njplastic.njplastic_api.production.enums.MachineState;

/**
 * Pure mapping helpers between production entities and the REST DTOs.
 * Stateless component so controllers and services can stay free of
 * conversion boilerplate.
 */
@Component
public class ProductionDtoMapper {

  /**
   * Convert a {@link Machine} to its summary DTO, embedding the current
   * operational state when known.
   *
   * @param machine      the source entity
   * @param currentState the latest open machine_status state, or null
   * @return the summary DTO
   */
  public MachineSummaryDTO toMachineSummary(Machine machine, MachineState currentState) {
    return MachineSummaryDTO.builder()
        .id(machine.getId())
        .code(machine.getCode())
        .description(machine.getDescription())
        .sector(machine.getSector())
        .standardCycleMs(machine.getStandardCycleMs())
        .consecutivePausesToStop(machine.getConsecutivePausesToStop())
        .active(machine.isActive())
        .currentState(currentState)
        .build();
  }

  /**
   * Convert a {@link MachineStatus} to its timeline DTO.
   *
   * @param status the source entity
   * @return the entry DTO
   */
  public MachineStatusEntryDTO toStatusEntry(MachineStatus status) {
    return MachineStatusEntryDTO.builder()
        .id(status.getId())
        .state(status.getState())
        .reason(status.getReason())
        .message(status.getMessage())
        .startTime(status.getStartTime())
        .endTime(status.getEndTime())
        .reasonAuthorId(status.getReasonAuthorId())
        .consecutiveCountAtCreation(status.getConsecutiveCountAtCreation())
        .build();
  }

  /**
   * Convert a list of status entities to their DTOs.
   *
   * @param records the source entities
   * @return the entry DTO list
   */
  public List<MachineStatusEntryDTO> toStatusEntries(List<MachineStatus> records) {
    return records.stream().map(this::toStatusEntry).toList();
  }

  /**
   * Convert a {@link ProductionCycle} to its response DTO.
   *
   * @param cycle the source entity
   * @return the response DTO
   */
  public ProductionCycleResponseDTO toCycleResponse(ProductionCycle cycle) {
    return ProductionCycleResponseDTO.builder()
        .id(cycle.getId())
        .machineId(cycle.getMachineId())
        .pulseTimestamp(cycle.getPulseTimestamp())
        .receivedAt(cycle.getReceivedAt())
        .sequence(cycle.getSequence())
        .intervalMs(cycle.getIntervalMs())
        .state(cycle.getState())
        .build();
  }
}
