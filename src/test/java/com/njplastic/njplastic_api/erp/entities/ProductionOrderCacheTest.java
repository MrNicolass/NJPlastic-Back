package com.njplastic.njplastic_api.erp.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ProductionOrderCacheTest {

  private ProductionOrderCache sample() {
    return ProductionOrderCache.builder()
        .id(UUID.fromString("2c3d4e5f-6a7b-8c9d-0e1f-2a3b4c5d6e7f"))
        .erpOrderId("OS-2026-00123")
        .machineId(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .productCode("PVC-100ML-BR")
        .targetQuantity(5000)
        .status("OPEN")
        .payload("{\"cliente\":\"Acme\"}")
        .lastSyncAt(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .build();
  }

  @Test
  void toString_includesKeyAttributes() {
    String text = sample().toString();
    assertThat(text)
        .contains("ProductionOrderCache{")
        .contains("erpOrderId=OS-2026-00123")
        .contains("productCode=PVC-100ML-BR")
        .contains("targetQuantity=5000")
        .contains("status=OPEN");
  }

  @Test
  void onCreate_generatesIdAndLastSyncAtWhenMissing() {
    ProductionOrderCache cache = new ProductionOrderCache();
    cache.onCreate();
    assertThat(cache.getId()).isNotNull();
    assertThat(cache.getLastSyncAt()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingIdAndLastSyncAt() {
    UUID id = UUID.randomUUID();
    OffsetDateTime existing = OffsetDateTime.parse("2026-05-01T10:00:00Z");
    ProductionOrderCache cache = ProductionOrderCache.builder().id(id).lastSyncAt(existing).build();
    cache.onCreate();
    assertThat(cache.getId()).isEqualTo(id);
    assertThat(cache.getLastSyncAt()).isEqualTo(existing);
  }

  @Test
  void settersUpdateFields() {
    ProductionOrderCache cache = new ProductionOrderCache();
    cache.setErpOrderId("OS-1");
    cache.setProductCode("SKU-1");
    cache.setTargetQuantity(100);
    cache.setStatus("OPEN");
    assertThat(cache.getErpOrderId()).isEqualTo("OS-1");
    assertThat(cache.getProductCode()).isEqualTo("SKU-1");
    assertThat(cache.getTargetQuantity()).isEqualTo(100);
    assertThat(cache.getStatus()).isEqualTo("OPEN");
  }
}
