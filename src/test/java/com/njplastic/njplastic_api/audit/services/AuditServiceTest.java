package com.njplastic.njplastic_api.audit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.njplastic.njplastic_api.audit.entities.AuditLog;
import com.njplastic.njplastic_api.audit.repositories.AuditRepository;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

  @Mock
  private AuditRepository auditRepository;

  @InjectMocks
  private AuditService auditService;

  private AuditLog sampleLog() {
    return AuditLog.builder()
        .httpMethod("POST")
        .endpoint("/auth/login")
        .httpStatus(200)
        .build();
  }

  @Test
  void saveAudit_delegatesToRepository() {
    AuditLog log = sampleLog();

    auditService.saveAudit(log);

    verify(auditRepository).save(log);
  }

  @Test
  void saveAudit_swallowsRepositoryFailure() {
    AuditLog log = sampleLog();
    doThrow(new RuntimeException("db down")).when(auditRepository).save(log);

    assertThatCode(() -> auditService.saveAudit(log)).doesNotThrowAnyException();

    verify(auditRepository).save(log);
  }

  @Test
  void findPaged_delegatesToRepositoryWithSpecification() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<AuditLog> page = new PageImpl<>(List.of(sampleLog()));
    when(auditRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    Page<AuditLog> result = auditService.findPaged(
        UUID.randomUUID(),
        "/auth",
        "POST",
        200,
        OffsetDateTime.parse("2026-05-28T00:00:00Z"),
        OffsetDateTime.parse("2026-05-29T00:00:00Z"),
        pageable);

    assertThat(result).isSameAs(page);
    verify(auditRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void findPaged_acceptsNullFilters() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<AuditLog> page = new PageImpl<>(List.of());
    when(auditRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    Page<AuditLog> result = auditService.findPaged(null, null, null, null, null, null, pageable);

    assertThat(result).isSameAs(page);
    verify(auditRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void findPaged_ignoresBlankStringFilters() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<AuditLog> page = new PageImpl<>(List.of());
    when(auditRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    auditService.findPaged(null, "  ", "", null, null, null, pageable);

    verify(auditRepository).findAll(any(Specification.class), eq(pageable));
  }
}
