package com.njplastic.njplastic_api.production.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.njplastic.njplastic_api.production.dtos.QualityRegistrationRequest;
import com.njplastic.njplastic_api.production.entities.QualityRecord;
import com.njplastic.njplastic_api.production.repositories.QualityRecordRepository;

@ExtendWith(MockitoExtension.class)
class QualityServiceTest {

  @Mock
  private QualityRecordRepository qualityRecordRepository;

  @InjectMocks
  private QualityService qualityService;

  private QualityRegistrationRequest request() {
    return QualityRegistrationRequest.builder()
        .machineId(UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .erpOrderId("OS-2026-00123")
        .periodStart(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
        .periodEnd(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .goodCount(950)
        .totalCount(1000)
        .build();
    }

  @Test
  void registerQuality_persistsBuiltRecordWithAuthor() {
    UUID author = UUID.randomUUID();
    when(qualityRecordRepository.save(any(QualityRecord.class))).thenAnswer(inv -> inv.getArgument(0));

    QualityRecord saved = qualityService.registerQuality(request(), author);

    ArgumentCaptor<QualityRecord> captor = ArgumentCaptor.forClass(QualityRecord.class);
    org.mockito.Mockito.verify(qualityRecordRepository).save(captor.capture());
    QualityRecord captured = captor.getValue();
    assertThat(captured.getMachineId()).isEqualTo(request().getMachineId());
    assertThat(captured.getErpOrderId()).isEqualTo("OS-2026-00123");
    assertThat(captured.getGoodCount()).isEqualTo(950);
    assertThat(captured.getTotalCount()).isEqualTo(1000);
    assertThat(captured.getRegisteredBy()).isEqualTo(author);
    assertThat(saved).isSameAs(captured);
  }

  @Test
  void registerQuality_allowsNullAuthor() {
    when(qualityRecordRepository.save(any(QualityRecord.class))).thenAnswer(inv -> inv.getArgument(0));

    QualityRecord saved = qualityService.registerQuality(request(), null);

    assertThat(saved.getRegisteredBy()).isNull();
  }

  @Test
  void findForPeriod_delegatesToRepository() {
    UUID machineId = UUID.randomUUID();
    OffsetDateTime from = OffsetDateTime.parse("2026-05-28T06:00:00Z");
    OffsetDateTime to = OffsetDateTime.parse("2026-05-28T14:00:00Z");
    QualityRecord record = QualityRecord.builder().id(UUID.randomUUID()).build();
    when(qualityRecordRepository
        .findByMachineIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(machineId, from, to))
        .thenReturn(List.of(record));

    assertThat(qualityService.findForPeriod(machineId, from, to)).containsExactly(record);
  }
}
