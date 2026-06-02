package com.njplastic.njplastic_api.erp.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.WRITE_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Replace-all payload for {@code PUT /erp/field-mapping}. Carries the full set
 * of mappings for a given entity_type; the service deletes the prior set and
 * persists the new one inside a single transaction. The diff is captured by
 * {@code AuditFilter} on the request/response payloads.
 */
@Schema(description = "Replace-all body for ERP field mapping under one entity_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpFieldMappingUpdateRequestDTO {

  @Schema(description = "Logical group these mappings belong to (e.g. PRODUCTION_ORDER)", example = "PRODUCTION_ORDER", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotBlank
  private String entityType;

  @Schema(description = "Full set of mappings to keep for this entity_type. Existing rows are dropped and replaced.", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
  @NotNull
  @Valid
  private List<MappingEntry> mappings;

  @Schema(description = "Single mapping entry inside the replace-all payload")
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MappingEntry {

    @Schema(description = "NJPlastic-side property name", example = "targetQuantity", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
    @NotBlank
    private String njField;

    @Schema(description = "ERP-side column name", example = "QTD_PROGRAMADA", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
    @NotBlank
    private String erpField;

    @Schema(description = "Data type used to bind the JDBC parameter", example = "INTEGER", requiredMode = REQUIRED, accessMode = WRITE_ONLY, nullable = false)
    @NotBlank
    private String dataType;

    @Schema(description = "Whether the mapping is mandatory on the ERP side", example = "true", requiredMode = NOT_REQUIRED, accessMode = WRITE_ONLY, nullable = false)
    private boolean required;

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("MappingEntry{njField=").append(njField)
          .append(", erpField=").append(erpField)
          .append(", dataType=").append(dataType)
          .append(", required=").append(required)
          .append('}');
      return sb.toString();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ErpFieldMappingUpdateRequestDTO{entityType=").append(entityType)
        .append(", mappings=").append(mappings)
        .append('}');
    return sb.toString();
  }
}
