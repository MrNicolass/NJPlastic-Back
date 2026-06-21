package com.njplastic.njplastic_api.erp.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.erp.entities.ErpSyncRun;
import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;

@Repository
public interface ErpSyncRunRepository extends JpaRepository<ErpSyncRun, UUID> {

  Optional<ErpSyncRun> findTopByOrderByStartedAtDesc();

  List<ErpSyncRun> findAllByOrderByStartedAtDesc(Pageable pageable);

  long countByStartedAtAfter(OffsetDateTime threshold);

  long countByStartedAtAfterAndStatus(OffsetDateTime threshold, ErpSyncStatus status);

 /**
 * Average duration of finished runs (any status) started after the given
 * threshold. Returns null when there are no rows in the window.
 *
 * @param threshold lower bound of started_at
 * @return average duration in milliseconds or null
 */
  @Query("""
      SELECT AVG(r.durationMs) FROM ErpSyncRun r
      WHERE r.startedAt > :threshold
        AND r.durationMs IS NOT NULL
      """)
  Double averageDurationMsAfter(@Param("threshold") OffsetDateTime threshold);
}