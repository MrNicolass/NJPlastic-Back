package com.njplastic.njplastic_api.reports.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.reports.entities.ReportSchedule;

@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, UUID> {

  List<ReportSchedule> findAllByActiveTrue();
}