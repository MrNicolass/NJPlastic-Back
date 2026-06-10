package com.njplastic.njplastic_api.production.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.production.enums.EventType;

class ProductionEventTest {

  private ProductionEvent sample() {
    return ProductionEvent.builder()
        .id(UUID.fromString("8a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d"))
        .machineId(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .userId(UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c"))
        .type(EventType.TRAINING)
        .description("Treinamento de novo operador")
        .startedAt(OffsetDateTime.parse("2026-06-01T10:00:00Z"))
        .endedAt(OffsetDateTime.parse("2026-06-01T11:30:00Z"))
        .build();
  }

  @Test
  void toString_includesTypeAndIds() {
    String text = sample().toString();
    assertThat(text)
        .contains("ProductionEvent{")
        .contains("type=TRAINING")
        .contains("machineId=9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d")
        .contains("userId=3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c");
  }

  @Test
  void onCreate_generatesIdAndCreatedAtWhenMissing() {
    ProductionEvent event = new ProductionEvent();
    event.onCreate();
    assertThat(event.getId()).isNotNull();
    assertThat(event.getCreatedAt()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingIdAndCreatedAt() {
    UUID id = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.now().minusHours(2);
    ProductionEvent event = ProductionEvent.builder().id(id).createdAt(created).build();
    event.onCreate();
    assertThat(event.getId()).isEqualTo(id);
    assertThat(event.getCreatedAt()).isEqualTo(created);
  }

  @Test
  void builderAndGetters_populateAllFields() {
    ProductionEvent event = sample();
    assertThat(event.getType()).isEqualTo(EventType.TRAINING);
    assertThat(event.getDescription()).isEqualTo("Treinamento de novo operador");
    assertThat(event.getStartedAt()).isEqualTo(OffsetDateTime.parse("2026-06-01T10:00:00Z"));
    assertThat(event.getEndedAt()).isEqualTo(OffsetDateTime.parse("2026-06-01T11:30:00Z"));
  }
}
