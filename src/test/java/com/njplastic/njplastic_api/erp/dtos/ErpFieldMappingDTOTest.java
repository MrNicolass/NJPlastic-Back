package com.njplastic.njplastic_api.erp.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.erp.entities.ErpFieldMapping;

class ErpFieldMappingDTOTest {

  @Test
  void from_copiesEveryField() {
    UUID id = UUID.fromString("6b5a4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d");
    UUID author = UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c");
    OffsetDateTime ts = OffsetDateTime.parse("2026-06-01T08:30:00Z");
    ErpFieldMapping entity = ErpFieldMapping.builder()
        .id(id)
        .entityType("PRODUCTION_ORDER")
        .njField("targetQuantity")
        .erpField("QTD_PROGRAMADA")
        .dataType("INTEGER")
        .required(true)
        .updatedBy(author)
        .updatedAt(ts)
        .build();

    ErpFieldMappingDTO dto = ErpFieldMappingDTO.from(entity);

    assertThat(dto.getId()).isEqualTo(id);
    assertThat(dto.getEntityType()).isEqualTo("PRODUCTION_ORDER");
    assertThat(dto.getNjField()).isEqualTo("targetQuantity");
    assertThat(dto.getErpField()).isEqualTo("QTD_PROGRAMADA");
    assertThat(dto.getDataType()).isEqualTo("INTEGER");
    assertThat(dto.isRequired()).isTrue();
    assertThat(dto.getUpdatedBy()).isEqualTo(author);
    assertThat(dto.getUpdatedAt()).isEqualTo(ts);
    assertThat(dto.toString()).contains("entityType=PRODUCTION_ORDER");
  }
}
