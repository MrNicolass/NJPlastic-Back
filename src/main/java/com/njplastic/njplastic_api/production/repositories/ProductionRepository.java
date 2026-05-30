package com.njplastic.njplastic_api.production.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.production.entities.ProductionCycle;
import com.njplastic.njplastic_api.production.enums.RecordState;

@Repository
public interface ProductionRepository extends JpaRepository<ProductionCycle, UUID> {

  Optional<ProductionCycle> findTopByMachineIdAndStateOrderByPulseTimestampDesc(UUID machineId, RecordState state);

  Optional<ProductionCycle> findTopByMachineIdOrderByReceivedAtDesc(UUID machineId);

  List<ProductionCycle> findByMachineIdAndStateOrderByPulseTimestampDesc(
      UUID machineId, RecordState state, Pageable pageable);

  long countByMachineIdAndStateAndPulseTimestampBetween(
      UUID machineId, RecordState state, OffsetDateTime from, OffsetDateTime to);
}