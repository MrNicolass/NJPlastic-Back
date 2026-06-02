package com.njplastic.njplastic_api.reports.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.reports.entities.ReportHistory;
import com.njplastic.njplastic_api.reports.enums.ReportType;

@Repository
public interface ReportHistoryRepository
    extends JpaRepository<ReportHistory, UUID>, JpaSpecificationExecutor<ReportHistory> {

  Page<ReportHistory> findByTypeAndGeneratedAtBetween(ReportType type, OffsetDateTime from, OffsetDateTime to, Pageable pageable);

  List<ReportHistory> findAllByGeneratedAtBefore(OffsetDateTime cutoff);
}
