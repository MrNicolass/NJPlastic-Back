package com.njplastic.njplastic_api.production.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.njplastic.njplastic_api.production.entities.ProductionEvent;
import com.njplastic.njplastic_api.production.enums.EventType;
import com.njplastic.njplastic_api.production.repositories.ProductionEventRepository;

@ExtendWith(MockitoExtension.class)
class ProductionEventServiceTest {

  @Mock
  private ProductionEventRepository repository;

  @InjectMocks
  private ProductionEventService service;

  @Test
  void register_savesEntityWithAllFields() {
    UUID machineId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    OffsetDateTime start = OffsetDateTime.parse("2026-06-01T10:00:00Z");
    OffsetDateTime end = OffsetDateTime.parse("2026-06-01T11:30:00Z");
    ProductionEvent expected = ProductionEvent.builder()
        .id(UUID.randomUUID())
        .machineId(machineId)
        .userId(userId)
        .type(EventType.TRAINING)
        .description("desc")
        .startedAt(start)
        .endedAt(end)
        .build();
    when(repository.save(any(ProductionEvent.class))).thenReturn(expected);

    ProductionEvent result = service.register(machineId, userId, EventType.TRAINING, "desc", start, end);

    ArgumentCaptor<ProductionEvent> captor = ArgumentCaptor.forClass(ProductionEvent.class);
    verify(repository).save(captor.capture());
    ProductionEvent saved = captor.getValue();
    assertThat(saved.getMachineId()).isEqualTo(machineId);
    assertThat(saved.getUserId()).isEqualTo(userId);
    assertThat(saved.getType()).isEqualTo(EventType.TRAINING);
    assertThat(saved.getDescription()).isEqualTo("desc");
    assertThat(saved.getStartedAt()).isEqualTo(start);
    assertThat(saved.getEndedAt()).isEqualTo(end);
    assertThat(result).isSameAs(expected);
  }

  @Test
  void findPaged_withoutWindow_delegatesToFindByMachineId() {
    UUID machineId = UUID.randomUUID();
    Pageable pageable = Pageable.unpaged();
    when(repository.findByMachineId(machineId, pageable)).thenReturn(new PageImpl<>(List.of()));

    service.findPaged(machineId, null, null, pageable);

    verify(repository).findByMachineId(machineId, pageable);
  }

  @Test
  void findPaged_withWindow_delegatesToBoundedQuery() {
    UUID machineId = UUID.randomUUID();
    OffsetDateTime from = OffsetDateTime.parse("2026-06-01T00:00:00Z");
    OffsetDateTime to = OffsetDateTime.parse("2026-06-01T23:59:59Z");
    Pageable pageable = Pageable.unpaged();
    when(repository.findByMachineIdAndStartedAtBetween(machineId, from, to, pageable))
        .thenReturn(new PageImpl<>(List.of()));

    service.findPaged(machineId, from, to, pageable);

    verify(repository).findByMachineIdAndStartedAtBetween(machineId, from, to, pageable);
  }
}
