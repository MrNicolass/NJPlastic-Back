package com.njplastic.njplastic_api.production.repositories;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.production.entities.ProductionEvent;

@Repository
public interface ProductionEventRepository extends JpaRepository<ProductionEvent, UUID> {

  Page<ProductionEvent> findByMachineIdAndStartedAtBetween(UUID machineId, OffsetDateTime from, OffsetDateTime to, Pageable pageable);

  Page<ProductionEvent> findByMachineId(UUID machineId, Pageable pageable);

  List<ProductionEvent> findByMachineIdInAndStartedAtBetweenOrderByStartedAtDesc(
      Collection<UUID> machineIds, OffsetDateTime from, OffsetDateTime to);
}
