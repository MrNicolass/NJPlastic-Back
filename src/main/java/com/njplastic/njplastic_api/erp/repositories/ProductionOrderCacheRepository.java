package com.njplastic.njplastic_api.erp.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.erp.entities.ProductionOrderCache;

@Repository
public interface ProductionOrderCacheRepository extends JpaRepository<ProductionOrderCache, UUID> {

  Optional<ProductionOrderCache> findByErpOrderId(String erpOrderId);
}