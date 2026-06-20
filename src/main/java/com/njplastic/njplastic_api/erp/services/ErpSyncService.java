package com.njplastic.njplastic_api.erp.services;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.erp.entities.ErpSyncRun;
import com.njplastic.njplastic_api.erp.entities.ProductionOrderCache;
import com.njplastic.njplastic_api.erp.enums.ErpSyncPhase;
import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;
import com.njplastic.njplastic_api.erp.exceptions.ErpDatabaseUnavailableException;
import com.njplastic.njplastic_api.erp.exceptions.ErpSyncDisabledException;
import com.njplastic.njplastic_api.erp.repositories.ErpDatabaseRepository;
import com.njplastic.njplastic_api.erp.repositories.ErpSyncRunRepository;
import com.njplastic.njplastic_api.erp.repositories.ProductionOrderCacheRepository;
import com.njplastic.njplastic_api.erp.repositories.rows.ErpOrderRow;
import com.njplastic.njplastic_api.erp.repositories.rows.MachineStatusRow;
import com.njplastic.njplastic_api.erp.repositories.rows.ProductionCycleRow;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.entities.ProductionCycle;
import com.njplastic.njplastic_api.production.services.MachineService;
import com.njplastic.njplastic_api.production.services.MachineStatusService;
import com.njplastic.njplastic_api.production.services.ProductionService;

/**
 * Orchestrates one ERP sync execution. Runs in
 * three phases ({@link ErpSyncPhase}): refresh the production_order_cache from
 * ERP open orders, push CONFIRMED production_cycle rows, push CONFIRMED
 * PAUSED/AUTO_STOPPED machine_status rows. On any failure inside a phase the
 * local record_state stays CONFIRMED and the run finishes as ERROR or
 * PARTIAL depending on the phase that broke. State transitions go through
 * {@code ProductionService} / {@code MachineStatusService} - this service never
 * touches the production repositories directly. This service also owns the
 * translation of JDBC failures into {@link ErpDatabaseUnavailableException} and
 * the conversion of domain types into JDBC parameter arrays - the repository
 * stays as a pure query executor.
 */
@Service
public class ErpSyncService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ErpSyncService.class);

  private final ObjectProvider<ErpDatabaseRepository> erpRepositoryProvider;
  private final ErpSyncRunRepository syncRunRepository;
  private final ProductionOrderCacheRepository orderCacheRepository;
  private final ProductionService productionService;
  private final MachineStatusService machineStatusService;
  private final MachineService machineService;
  private final int batchPageSize;

  public ErpSyncService(
      ObjectProvider<ErpDatabaseRepository> erpRepositoryProvider,
      ErpSyncRunRepository syncRunRepository,
      ProductionOrderCacheRepository orderCacheRepository,
      ProductionService productionService,
      MachineStatusService machineStatusService,
      MachineService machineService,
      @Value("${app.erp.sync.batch-page-size:200}") int batchPageSize) {
    this.erpRepositoryProvider = erpRepositoryProvider;
    this.syncRunRepository = syncRunRepository;
    this.orderCacheRepository = orderCacheRepository;
    this.productionService = productionService;
    this.machineStatusService = machineStatusService;
    this.machineService = machineService;
    this.batchPageSize = batchPageSize;
  }

 /**
 * Execute one full sync window: refresh the order cache, push confirmed
 * cycles, push confirmed downtime. Persists the outcome in {@code erp_sync_run}
 * regardless of success or failure. Throws
 * {@link ErpSyncDisabledException} (422) when the ERP datasource is
 * disabled - the scheduler is conditional on the same flag, so this path is
 * only reached from a manual call (e.g. a future "force sync" endpoint).
 *
 * @return the persisted ErpSyncRun row, useful for tests and future force-sync
 */
  public ErpSyncRun runSync() {
    ErpDatabaseRepository erp = erpRepositoryProvider.getIfAvailable();
    if (erp == null) {
      throw new ErpSyncDisabledException(
          "ERP datasource is disabled (app.datasource.erp.enabled=false)");
    }

    ErpSyncRun run = ErpSyncRun.builder()
        .startedAt(OffsetDateTime.now())
        .status(ErpSyncStatus.RUNNING)
        .build();
    run = syncRunRepository.save(run);

    int ordersRead = 0;
    int cyclesWritten = 0;
    int pausesWritten = 0;
    ErpSyncStatus finalStatus = ErpSyncStatus.SUCCESS;
    String errorMessage = null;

    try {
      ordersRead = refreshOrderCache(erp);
    } catch (DataAccessException ex) {
      errorMessage = recordPhaseError(ErpSyncPhase.REFRESH_ORDERS, ex);
      finalStatus = ErpSyncStatus.ERROR;
      return finalizeRun(run, ordersRead, cyclesWritten, pausesWritten, finalStatus, errorMessage);
    }

    try {
      cyclesWritten = pushConfirmedCycles(erp);
    } catch (DataAccessException ex) {
      errorMessage = recordPhaseError(ErpSyncPhase.PUSH_CYCLES, ex);
      finalStatus = ErpSyncStatus.PARTIAL;
      return finalizeRun(run, ordersRead, cyclesWritten, pausesWritten, finalStatus, errorMessage);
    }

    try {
      pausesWritten = pushConfirmedDowntime(erp);
    } catch (DataAccessException ex) {
      errorMessage = recordPhaseError(ErpSyncPhase.PUSH_PAUSES, ex);
      finalStatus = ErpSyncStatus.PARTIAL;
    }

    return finalizeRun(run, ordersRead, cyclesWritten, pausesWritten, finalStatus, errorMessage);
  }

  private int refreshOrderCache(ErpDatabaseRepository erp) {
    List<ErpOrderRow> orders = erp.findOpenOrders();
    OffsetDateTime now = OffsetDateTime.now();
    for (ErpOrderRow order : orders) {
      ProductionOrderCache cache = orderCacheRepository.findByErpOrderId(order.erpOrderId())
          .orElseGet(() -> ProductionOrderCache.builder()
              .erpOrderId(order.erpOrderId())
              .build());
      cache.setMachineId(resolveMachineIdByCode(order.machineCode()).orElse(cache.getMachineId()));
      cache.setProductCode(order.productCode());
      cache.setTargetQuantity(order.targetQuantity());
      cache.setStatus(order.status());
      cache.setPayload(order.payloadJson());
      cache.setLastSyncAt(now);
      orderCacheRepository.save(cache);
    }
    return orders.size();
  }

  private int pushConfirmedCycles(ErpDatabaseRepository erp) {
    List<ProductionCycle> cycles = productionService
        .findConfirmedAwaitingSync(PageRequest.of(0, batchPageSize));
    if (cycles.isEmpty()) {
      return 0;
    }
    Map<UUID, String> machineCodeCache = new HashMap<>();
    List<ProductionCycle> writtenCycles = new ArrayList<>(cycles.size());
    for (ProductionCycle cycle : cycles) {
      String machineCode = resolveMachineCode(cycle.getMachineId(), machineCodeCache);
      if (machineCode == null) {
        LOGGER.warn("Skipping cycle {}: machine {} unknown", cycle.getId(), cycle.getMachineId());
        continue;
      }
      ProductionCycleRow row = new ProductionCycleRow(
          cycle.getId(),
          machineCode,
          cycle.getPulseTimestamp(),
          cycle.getSequence(),
          cycle.getIntervalMs());
      erp.insertCycle(toCycleParams(row));
      writtenCycles.add(cycle);
    }
    productionService.markCyclesAsSynced(writtenCycles);
    return writtenCycles.size();
  }

  private int pushConfirmedDowntime(ErpDatabaseRepository erp) {
    List<MachineStatus> records = machineStatusService
        .findConfirmedDowntimeAwaitingSync(PageRequest.of(0, batchPageSize));
    if (records.isEmpty()) {
      return 0;
    }
    Map<UUID, String> machineCodeCache = new HashMap<>();
    List<MachineStatus> writtenRecords = new ArrayList<>(records.size());
    for (MachineStatus record : records) {
      String machineCode = resolveMachineCode(record.getMachineId(), machineCodeCache);
      if (machineCode == null) {
        LOGGER.warn("Skipping status {}: machine {} unknown", record.getId(), record.getMachineId());
        continue;
      }
      MachineStatusRow row = new MachineStatusRow(
          record.getId(),
          machineCode,
          record.getState().name(),
          record.getReason(),
          record.getMessage(),
          record.getStartTime(),
          record.getEndTime(),
          record.getConsecutiveCountAtCreation());
      erp.insertPause(toPauseParams(row), pauseParamTypes());
      writtenRecords.add(record);
    }
    machineStatusService.markStatusesAsSynced(writtenRecords);
    return writtenRecords.size();
  }

  private Object[] toCycleParams(ProductionCycleRow row) {
    return new Object[] {
        row.id().toString(),
        row.machineCode(),
        row.pulseTimestamp() == null ? null : Timestamp.from(row.pulseTimestamp().toInstant()),
        row.sequence(),
        row.intervalMs()
    };
  }

  private Object[] toPauseParams(MachineStatusRow row) {
    return new Object[] {
        row.id().toString(),
        row.machineCode(),
        row.state(),
        row.reason(),
        row.message(),
        row.startTime() == null ? null : Timestamp.from(row.startTime().toInstant()),
        row.endTime() == null ? null : Timestamp.from(row.endTime().toInstant()),
        row.consecutiveCount()
    };
  }

  private int[] pauseParamTypes() {
    return new int[] {
        Types.VARCHAR,
        Types.VARCHAR,
        Types.VARCHAR,
        Types.VARCHAR,
        Types.VARCHAR,
        Types.TIMESTAMP,
        Types.TIMESTAMP,
        Types.INTEGER
    };
  }

  private String recordPhaseError(ErpSyncPhase phase, DataAccessException cause) {
    ErpDatabaseUnavailableException translated = new ErpDatabaseUnavailableException(
        "ERP " + phase.getDescription() + " query failed", cause);
    LOGGER.warn("ERP sync phase '{}' failed: {}", phase.getDescription(), translated.getMessage());
    return phase.getDescription() + ": " + translated.getMessage();
  }

  private ErpSyncRun finalizeRun(ErpSyncRun run, int ordersRead, int cyclesWritten, int pausesWritten,
      ErpSyncStatus status, String errorMessage) {
    OffsetDateTime finishedAt = OffsetDateTime.now();
    long durationMs = Duration.between(run.getStartedAt(), finishedAt).toMillis();
    run.setFinishedAt(finishedAt);
    run.setStatus(status);
    run.setOrdersRead(ordersRead);
    run.setCyclesWritten(cyclesWritten);
    run.setPausesWritten(pausesWritten);
    run.setDurationMs((int) Math.min(durationMs, Integer.MAX_VALUE));
    run.setErrorMessage(errorMessage);
    return syncRunRepository.save(run);
  }

  private String resolveMachineCode(UUID machineId, Map<UUID, String> cache) {
    if (machineId == null) {
      return null;
    }
    if (cache.containsKey(machineId)) {
      return cache.get(machineId);
    }
    String code = machineService.findById(machineId).map(Machine::getCode).orElse(null);
    cache.put(machineId, code);
    return code;
  }

  private Optional<UUID> resolveMachineIdByCode(String machineCode) {
    if (machineCode == null || machineCode.isBlank()) {
      return Optional.empty();
    }
    return machineService.findActiveByCode(machineCode).map(Machine::getId);
  }
}
