package com.njplastic.njplastic_api.erp.schedulers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.njplastic.njplastic_api.erp.services.ErpSyncService;

import lombok.RequiredArgsConstructor;

/**
 * Periodically triggers {@link ErpSyncService#runSync()} on the configured
 * window (RF14, RNF03). Created only when
 * {@code app.datasource.erp.enabled=true}; {@code @EnableScheduling} is
 * already active through {@code MqttConfig}. Failures inside the sync flow
 * are translated into ErpSyncRun rows by the service - the catch here is a
 * last-resort guard so an unexpected RuntimeException does not break the
 * Spring scheduler thread.
 */
@Component
@ConditionalOnProperty(prefix = "app.datasource.erp", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ErpSyncScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ErpSyncScheduler.class);

  private final ErpSyncService erpSyncService;

  @Scheduled(fixedDelayString = "${app.erp.sync.fixed-delay-ms}", initialDelayString = "${app.erp.sync.initial-delay-ms}")
  public void runScheduledSync() {
    try {
      erpSyncService.runSync();
    } catch (Exception ex) {
      LOGGER.error("ERP sync scheduled run aborted unexpectedly", ex);
    }
  }
}