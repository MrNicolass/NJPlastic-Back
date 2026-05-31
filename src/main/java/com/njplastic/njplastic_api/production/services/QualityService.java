package com.njplastic.njplastic_api.production.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.njplastic.njplastic_api.production.dtos.QualityRegistrationRequestDTO;
import com.njplastic.njplastic_api.production.entities.QualityRecord;
import com.njplastic.njplastic_api.production.repositories.QualityRecordRepository;

import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link QualityRecordRepository}. Stores the good/total counts
 * a
 * user registers at the end of a production order and exposes them to OEE
 * (RF10).
 */
@Service
@RequiredArgsConstructor
public class QualityService {

  private final QualityRecordRepository qualityRecordRepository;

  /**
   * Persist quality counts for a machine/period.
   *
   * @param request  the counts and the period they cover
   * @param authorId the user registering the counts, or null when unknown
   * @return the stored record
   */
  @Transactional
  public QualityRecord registerQuality(QualityRegistrationRequestDTO request, UUID authorId) {
    QualityRecord record = QualityRecord.builder()
        .machineId(request.getMachineId())
        .erpOrderId(request.getErpOrderId())
        .periodStart(request.getPeriodStart())
        .periodEnd(request.getPeriodEnd())
        .goodCount(request.getGoodCount())
        .totalCount(request.getTotalCount())
        .registeredBy(authorId)
        .build();
    return qualityRecordRepository.save(record);
  }

  /**
   * Quality records fully contained in the given window, used by OEE.
   *
   * @param machineId the machine UUID
   * @param from      window start
   * @param to        window end
   * @return matching records (possibly empty)
   */
  public List<QualityRecord> findForPeriod(UUID machineId, OffsetDateTime from, OffsetDateTime to) {
    return qualityRecordRepository
        .findByMachineIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(machineId, from, to);
  }
}