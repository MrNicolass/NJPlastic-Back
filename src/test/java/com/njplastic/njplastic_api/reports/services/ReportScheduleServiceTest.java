package com.njplastic.njplastic_api.reports.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.njplastic.njplastic_api.config.exceptions.BaseApiNotFoundException;
import com.njplastic.njplastic_api.reports.entities.ReportHistory;
import com.njplastic.njplastic_api.reports.entities.ReportSchedule;
import com.njplastic.njplastic_api.reports.enums.ReportFormat;
import com.njplastic.njplastic_api.reports.enums.ReportType;
import com.njplastic.njplastic_api.reports.repositories.ReportHistoryRepository;
import com.njplastic.njplastic_api.reports.repositories.ReportScheduleRepository;

@ExtendWith(MockitoExtension.class)
class ReportScheduleServiceTest {

  @Mock
  private ReportScheduleRepository scheduleRepository;

  @Mock
  private ReportHistoryRepository historyRepository;

  @InjectMocks
  private ReportScheduleService service;

  private ReportSchedule schedule() {
    return ReportSchedule.builder()
        .id(UUID.randomUUID())
        .type(ReportType.SHIFT)
        .cron("0 0 7 * * MON-FRI")
        .deliveryEmail("m@njplastic.com")
        .format(ReportFormat.CSV)
        .active(false)
        .build();
  }

  @Test
  void create_forcesActiveAndPersists() {
    ReportSchedule input = schedule();
    input.setActive(false);
    when(scheduleRepository.save(any(ReportSchedule.class))).thenAnswer(i -> i.getArgument(0));

    ReportSchedule result = service.create(input);

    assertThat(result.isActive()).isTrue();
    verify(scheduleRepository).save(input);
  }

  @Test
  void delete_throws404WhenNotFound() {
    UUID id = UUID.randomUUID();
    when(scheduleRepository.existsById(id)).thenReturn(false);

    assertThatThrownBy(() -> service.delete(id))
        .isInstanceOf(BaseApiNotFoundException.class);
  }

  @Test
  void delete_removesWhenFound() {
    UUID id = UUID.randomUUID();
    when(scheduleRepository.existsById(id)).thenReturn(true);

    service.delete(id);

    verify(scheduleRepository).deleteById(id);
  }

  @Test
  void findAllActive_delegatesToRepository() {
    List<ReportSchedule> schedules = List.of(schedule());
    when(scheduleRepository.findAllByActiveTrue()).thenReturn(schedules);

    List<ReportSchedule> result = service.findAllActive();

    assertThat(result).isSameAs(schedules);
  }

  @Test
  void recordGeneration_persists() {
    ReportHistory history = ReportHistory.builder()
        .id(UUID.randomUUID())
        .type(ReportType.SHIFT)
        .format(ReportFormat.CSV)
        .generatedAt(OffsetDateTime.now())
        .path("./reports/2026/06/01/shift_07-00.csv")
        .sizeBytes(1024L)
        .build();
    when(historyRepository.save(history)).thenReturn(history);

    ReportHistory result = service.recordGeneration(history);

    assertThat(result).isSameAs(history);
    verify(historyRepository).save(history);
  }

  @Test
  @SuppressWarnings("unchecked")
  void findHistory_withFilters_returnsPage() {
    Pageable pageable = Pageable.unpaged();
    OffsetDateTime from = OffsetDateTime.parse("2026-06-01T00:00:00Z");
    OffsetDateTime to = OffsetDateTime.parse("2026-06-01T23:59:59Z");
    when(historyRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));

    service.findHistory(ReportType.SHIFT, from, to, pageable);

    verify(historyRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void deleteHistory_delegatesById() {
    UUID id = UUID.randomUUID();

    service.deleteHistory(id);

    verify(historyRepository).deleteById(id);
  }
}
