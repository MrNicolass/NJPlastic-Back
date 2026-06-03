package com.njplastic.njplastic_api.erp.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.erp.entities.ErpFieldMapping;

@Repository
public interface ErpFieldMappingRepository extends JpaRepository<ErpFieldMapping, UUID> {

  List<ErpFieldMapping> findAllByEntityTypeOrderByNjFieldAsc(String entityType);

  void deleteByEntityType(String entityType);
}
