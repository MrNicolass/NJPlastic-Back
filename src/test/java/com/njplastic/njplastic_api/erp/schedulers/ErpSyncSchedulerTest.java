package com.njplastic.njplastic_api.erp.schedulers;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.njplastic.njplastic_api.erp.services.ErpSyncService;

@ExtendWith(MockitoExtension.class)
class ErpSyncSchedulerTest {

  @Mock
  private ErpSyncService erpSyncService;

  @InjectMocks
  private ErpSyncScheduler scheduler;

  @Test
  void runScheduledSync_delegatesToService() {
    scheduler.runScheduledSync();

    verify(erpSyncService).runSync();
  }

  @Test
  void runScheduledSync_swallowsUnexpectedRuntimeException() {
    when(erpSyncService.runSync()).thenThrow(new RuntimeException("boom"));

    assertThatCode(() -> scheduler.runScheduledSync()).doesNotThrowAnyException();
    verify(erpSyncService).runSync();
  }
}
