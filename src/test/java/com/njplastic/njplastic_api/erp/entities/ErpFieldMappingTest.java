package com.njplastic.njplastic_api.erp.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ErpFieldMappingTest {

  @Test
  void onCreate_generatesIdAndUpdatedAtWhenMissing() {
    ErpFieldMapping mapping = ErpFieldMapping.builder()
        .entityType("PRODUCTION_ORDER")
        .njField("targetQuantity")
        .erpField("QTD_PROGRAMADA")
        .dataType("INTEGER")
        .build();

    mapping.onCreate();

    assertThat(mapping.getId()).isNotNull();
    assertThat(mapping.getUpdatedAt()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingValues() {
    UUID id = UUID.randomUUID();
    OffsetDateTime ts = OffsetDateTime.parse("2026-06-01T08:30:00Z");
    ErpFieldMapping mapping = ErpFieldMapping.builder()
        .id(id)
        .updatedAt(ts)
        .entityType("PRODUCTION_ORDER")
        .njField("targetQuantity")
        .erpField("QTD_PROGRAMADA")
        .dataType("INTEGER")
        .build();

    mapping.onCreate();

    assertThat(mapping.getId()).isEqualTo(id);
    assertThat(mapping.getUpdatedAt()).isEqualTo(ts);
  }

  @Test
  void onUpdate_refreshesUpdatedAt() {
    OffsetDateTime ts = OffsetDateTime.parse("2020-01-01T00:00:00Z");
    ErpFieldMapping mapping = ErpFieldMapping.builder()
        .id(UUID.randomUUID())
        .updatedAt(ts)
        .entityType("X")
        .njField("y")
        .erpField("Y")
        .dataType("VARCHAR")
        .build();

    mapping.onUpdate();

    assertThat(mapping.getUpdatedAt()).isAfter(ts);
  }

  @Test
  void toString_carriesIdentifyingFields() {
    ErpFieldMapping mapping = ErpFieldMapping.builder()
        .id(UUID.fromString("6b5a4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d"))
        .entityType("PRODUCTION_ORDER")
        .njField("targetQuantity")
        .erpField("QTD_PROGRAMADA")
        .dataType("INTEGER")
        .required(true)
        .build();

    String text = mapping.toString();

    assertThat(text).contains("entityType=PRODUCTION_ORDER");
    assertThat(text).contains("njField=targetQuantity");
    assertThat(text).contains("erpField=QTD_PROGRAMADA");
    assertThat(text).contains("dataType=INTEGER");
    assertThat(text).contains("required=true");
  }
}
