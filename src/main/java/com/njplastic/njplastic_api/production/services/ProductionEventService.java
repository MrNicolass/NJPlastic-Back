package com.njplastic.njplastic_api.production.services;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.njplastic.njplastic_api.production.entities.ProductionEvent;
import com.njplastic.njplastic_api.production.enums.EventType;
import com.njplastic.njplastic_api.production.repositories.ProductionEventRepository;

import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link ProductionEventRepository} (EP-BE-05 reopened).
 * Persists manual production events (training, cleaning, meetings) and exposes
 * a paginated read keyed by machine.
 */
@Service
@RequiredArgsConstructor
public class ProductionEventService {

  private final ProductionEventRepository repository;

  /**
   * Persist a new manual production event.
   *
   * @param machineId   target machine UUID (must already be access-checked by the caller)
   * @param userId      author UUID; null when no human author applies
   * @param type        event category
   * @param description free-text description
   * @param startedAt   moment the event started (UTC)
   * @param endedAt     moment the event ended; null while ongoing
   * @return the persisted entity
   */
  @Transactional
  public ProductionEvent register(UUID machineId, UUID userId, EventType type, String description,
      OffsetDateTime startedAt, OffsetDateTime endedAt) {
    ProductionEvent event = ProductionEvent.builder()
        .machineId(machineId)
        .userId(userId)
        .type(type)
        .description(description)
        .startedAt(startedAt)
        .endedAt(endedAt)
        .build();
    return repository.save(event);
  }

  /**
   * Paginated read of events for a machine, optionally constrained to a time
   * window.
   *
   * @param machineId machine UUID
   * @param from      optional inclusive start of window
   * @param to        optional exclusive end of window
   * @param pageable  paging/sort
   * @return page of events
   */
  public Page<ProductionEvent> findPaged(UUID machineId, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
    if (from != null && to != null) {
      return repository.findByMachineIdAndStartedAtBetween(machineId, from, to, pageable);
    }
    return repository.findByMachineId(machineId, pageable);
  }
}
