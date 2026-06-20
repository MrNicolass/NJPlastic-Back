package com.njplastic.njplastic_api.production.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.production.entities.QualityRecord;

@Repository
public interface QualityRecordRepository extends JpaRepository<QualityRecord, UUID> {

 /**
 * Quality records that overlap the window [from, to]. A record is included
 * when its own period intersects the window in any way, so a record stored
 * for a slightly different period (e.g. the user submitted [T-8h, T] but
 * the dashboard window already advanced to [T-8h+5s, T+5s]) still
 * contributes to the OEE quality factor.
 */
  List<QualityRecord> findByMachineIdAndPeriodStartLessThanAndPeriodEndGreaterThan(
      UUID machineId, OffsetDateTime windowEnd, OffsetDateTime windowStart);
}