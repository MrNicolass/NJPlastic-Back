package com.njplastic.njplastic_api.production.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.production.entities.QualityRecord;

@Repository
public interface QualityRecordRepository extends JpaRepository<QualityRecord, UUID> {

  List<QualityRecord> findByMachineIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
      UUID machineId, OffsetDateTime periodStart, OffsetDateTime periodEnd);
}