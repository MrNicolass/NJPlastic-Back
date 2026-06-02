package com.njplastic.njplastic_api.erp.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.njplastic.njplastic_api.erp.dtos.ErpFieldMappingUpdateRequestDTO;
import com.njplastic.njplastic_api.erp.entities.ErpFieldMapping;
import com.njplastic.njplastic_api.erp.repositories.ErpFieldMappingRepository;

import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link ErpFieldMappingRepository} (EP-BE-06 reopened). Read
 * returns the mapping set for a given entity_type; the write replaces the
 * full set atomically and stamps the author. The diff is captured by
 * {@code AuditFilter} on the request/response payloads.
 */
@Service
@RequiredArgsConstructor
public class ErpFieldMappingService {

  private final ErpFieldMappingRepository repository;

  /**
   * Read every mapping for the given entity_type, sorted by njField.
   *
   * @param entityType logical group identifier (e.g. PRODUCTION_ORDER)
   * @return mapping rows
   */
  public List<ErpFieldMapping> getMapping(String entityType) {
    return repository.findAllByEntityTypeOrderByNjFieldAsc(entityType);
  }

  /**
   * Replace every mapping under the entity_type with the supplied list.
   * Deletes the prior set and inserts the new one inside the same transaction
   * so partial failures roll back.
   *
   * @param request   payload carrying entity_type and the new mapping list
   * @param updatedBy author UUID resolved from the JWT
   * @return the persisted mapping list
   */
  @Transactional
  public List<ErpFieldMapping> replaceMapping(ErpFieldMappingUpdateRequestDTO request, UUID updatedBy) {
    repository.deleteByEntityType(request.getEntityType());
    OffsetDateTime now = OffsetDateTime.now();
    List<ErpFieldMapping> entities = request.getMappings().stream()
        .map(entry -> ErpFieldMapping.builder()
            .entityType(request.getEntityType())
            .njField(entry.getNjField())
            .erpField(entry.getErpField())
            .dataType(entry.getDataType())
            .required(entry.isRequired())
            .updatedBy(updatedBy)
            .updatedAt(now)
            .build())
        .toList();
    return repository.saveAll(entities);
  }
}
