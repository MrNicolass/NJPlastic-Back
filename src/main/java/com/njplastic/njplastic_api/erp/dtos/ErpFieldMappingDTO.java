package com.njplastic.njplastic_api.erp.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.njplastic.njplastic_api.erp.entities.ErpFieldMapping;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Read-side projection of an {@link ErpFieldMapping}. Used in the response of
 * {@code GET /erp/field-mapping} and as the element type inside
 * {@code PUT /erp/field-mapping}'s replace-all body.
 */
@Schema(description = "ERP field mapping projection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpFieldMappingDTO {

  @Schema(description = "Mapping UUID", example = "6b5a4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private UUID id;

  @Schema(description = "Logical group of mappings (e.g. PRODUCTION_ORDER)", example = "PRODUCTION_ORDER", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String entityType;

  @Schema(description = "NJPlastic-side property name", example = "targetQuantity", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String njField;

  @Schema(description = "ERP-side column name", example = "QTD_PROGRAMADA", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String erpField;

  @Schema(description = "Data type used to bind the JDBC parameter", example = "INTEGER", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String dataType;

  @Schema(description = "Whether the mapping is mandatory on the ERP side", example = "true", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private boolean required;

  @Schema(description = "Author of the last update", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private UUID updatedBy;

  @Schema(description = "Last update timestamp", example = "2026-06-01T08:30:00Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime updatedAt;

  /**
   * @param entity source entity
   * @return populated DTO
   */
  public static ErpFieldMappingDTO from(ErpFieldMapping entity) {
    return ErpFieldMappingDTO.builder()
        .id(entity.getId())
        .entityType(entity.getEntityType())
        .njField(entity.getNjField())
        .erpField(entity.getErpField())
        .dataType(entity.getDataType())
        .required(entity.isRequired())
        .updatedBy(entity.getUpdatedBy())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ErpFieldMappingDTO{id=").append(id)
        .append(", entityType=").append(entityType)
        .append(", njField=").append(njField)
        .append(", erpField=").append(erpField)
        .append(", dataType=").append(dataType)
        .append(", required=").append(required)
        .append(", updatedBy=").append(updatedBy)
        .append(", updatedAt=").append(updatedAt)
        .append('}');
    return sb.toString();
  }
}
