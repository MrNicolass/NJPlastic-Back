package com.njplastic.njplastic_api.audit.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
