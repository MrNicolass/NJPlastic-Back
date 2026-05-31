package com.njplastic.njplastic_api.erp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Pageable;

import com.njplastic.njplastic_api.erp.entities.ErpSyncRun;
import com.njplastic.njplastic_api.erp.entities.ProductionOrderCache;
import com.njplastic.njplastic_api.erp.enums.ErpSyncStatus;
import com.njplastic.njplastic_api.erp.exceptions.ErpSyncDisabledException;
import com.njplastic.njplastic_api.erp.repositories.ErpDatabaseRepository;
import com.njplastic.njplastic_api.erp.repositories.ErpSyncRunRepository;
import com.njplastic.njplastic_api.erp.repositories.ProductionOrderCacheRepository;
import com.njplastic.njplastic_api.erp.repositories.rows.ErpOrderRow;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.entities.ProductionCycle;
import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecordState;
import com.njplastic.njplastic_api.production.services.MachineService;
import com.njplastic.njplastic_api.production.services.MachineStatusService;
import com.njplastic.njplastic_api.production.services.ProductionService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ErpSyncServiceTest {

  @Mock
  private ObjectProvider<ErpDatabaseRepository> erpRepositoryProvider;

  @Mock
  private ErpDatabaseRepository erp;

  @Mock
  private ErpSyncRunRepository syncRunRepository;

  @Mock
  private ProductionOrderCacheRepository orderCacheRepository;

  @Mock
  private ProductionService productionService;

  @Mock
  private MachineStatusService machineStatusService;

  @Mock
  private MachineService machineService;

  private ErpSyncService service;

  private static final UUID MACHINE_ID = UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d");
  private static final UUID UNKNOWN_MACHINE_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
  private static final int BATCH_PAGE_SIZE = 200;

  @BeforeEach
  void setUp() {
    service = new ErpSyncService(erpRepositoryProvider, syncRunRepository, orderCacheRepository,
        productionService, machineStatusService, machineService, BATCH_PAGE_SIZE);
    when(syncRunRepository.save(any(ErpSyncRun.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  private Machine machine() {
    return Machine.builder().id(MACHINE_ID).code("MAQ-01").build();
  }

  @Test
  void runSync_throwsWhenErpDatasourceDisabled() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(null);

    assertThatThrownBy(() -> service.runSync())
        .isInstanceOf(ErpSyncDisabledException.class)
        .hasMessageContaining("disabled");
    verify(syncRunRepository, never()).save(any());
  }

  @Test
  void runSync_successPathSavesRunningThenSuccess() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenReturn(List.of(
        new ErpOrderRow("OS-1", "MAQ-01", "SKU-1", 100, "OPEN", "{}")));
    when(orderCacheRepository.findByErpOrderId("OS-1")).thenReturn(Optional.empty());
    when(machineService.findActiveByCode("MAQ-01")).thenReturn(Optional.of(machine()));
    when(productionService.findConfirmedAwaitingSync(any(Pageable.class))).thenReturn(List.of());
    when(machineStatusService.findConfirmedDowntimeAwaitingSync(any(Pageable.class))).thenReturn(List.of());

    ErpSyncRun result = service.runSync();

    assertThat(result.getStatus()).isEqualTo(ErpSyncStatus.SUCCESS);
    assertThat(result.getOrdersRead()).isEqualTo(1);
    assertThat(result.getCyclesWritten()).isEqualTo(0);
    assertThat(result.getPausesWritten()).isEqualTo(0);
    assertThat(result.getFinishedAt()).isNotNull();
    assertThat(result.getDurationMs()).isNotNull();
    assertThat(result.getErrorMessage()).isNull();
    verify(syncRunRepository, times(2)).save(any(ErpSyncRun.class));
    verify(orderCacheRepository).save(any(ProductionOrderCache.class));
  }

  @Test
  void runSync_refreshOrderCache_updatesExistingRowAndKeepsMachineIdWhenCodeUnknown() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenReturn(List.of(
        new ErpOrderRow("OS-1", "MAQ-UNK", "SKU-1", 50, "OPEN", "{}")));
    ProductionOrderCache existing = ProductionOrderCache.builder()
        .id(UUID.randomUUID())
        .erpOrderId("OS-1")
        .machineId(MACHINE_ID)
        .build();
    when(orderCacheRepository.findByErpOrderId("OS-1")).thenReturn(Optional.of(existing));
    when(machineService.findActiveByCode("MAQ-UNK")).thenReturn(Optional.empty());
    when(productionService.findConfirmedAwaitingSync(any(Pageable.class))).thenReturn(List.of());
    when(machineStatusService.findConfirmedDowntimeAwaitingSync(any(Pageable.class))).thenReturn(List.of());

    service.runSync();

    ArgumentCaptor<ProductionOrderCache> captor = ArgumentCaptor.forClass(ProductionOrderCache.class);
    verify(orderCacheRepository).save(captor.capture());
    ProductionOrderCache saved = captor.getValue();
    assertThat(saved).isSameAs(existing);
    assertThat(saved.getMachineId()).isEqualTo(MACHINE_ID);
    assertThat(saved.getProductCode()).isEqualTo("SKU-1");
    assertThat(saved.getTargetQuantity()).isEqualTo(50);
    assertThat(saved.getStatus()).isEqualTo("OPEN");
  }

  @Test
  void runSync_refreshFailureReturnsErrorAndSkipsLaterPhases() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenThrow(new DataAccessResourceFailureException("erp down"));

    ErpSyncRun result = service.runSync();

    assertThat(result.getStatus()).isEqualTo(ErpSyncStatus.ERROR);
    assertThat(result.getOrdersRead()).isZero();
    assertThat(result.getErrorMessage()).contains("refresh-orders");
    verify(productionService, never()).findConfirmedAwaitingSync(any());
    verify(machineStatusService, never()).findConfirmedDowntimeAwaitingSync(any());
  }

  @Test
  void runSync_pushCyclesWritesEachAndMarksAsSynced() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenReturn(List.of());
    UUID cycleId = UUID.randomUUID();
    ProductionCycle cycle = ProductionCycle.builder()
        .id(cycleId)
        .machineId(MACHINE_ID)
        .pulseTimestamp(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .sequence(7L)
        .intervalMs(2000)
        .state(RecordState.CONFIRMED)
        .build();
    when(productionService.findConfirmedAwaitingSync(any(Pageable.class))).thenReturn(List.of(cycle));
    when(machineService.findById(MACHINE_ID)).thenReturn(Optional.of(machine()));
    when(machineStatusService.findConfirmedDowntimeAwaitingSync(any(Pageable.class))).thenReturn(List.of());

    ErpSyncRun result = service.runSync();

    assertThat(result.getStatus()).isEqualTo(ErpSyncStatus.SUCCESS);
    assertThat(result.getCyclesWritten()).isEqualTo(1);
    verify(erp).insertCycle(any(Object[].class));
    verify(productionService).markCyclesAsSynced(List.of(cycle));
  }

  @Test
  void runSync_pushCyclesSkipsCyclesWhenMachineUnknown() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenReturn(List.of());
    ProductionCycle cycle = ProductionCycle.builder()
        .id(UUID.randomUUID())
        .machineId(UNKNOWN_MACHINE_ID)
        .pulseTimestamp(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .sequence(1L)
        .state(RecordState.CONFIRMED)
        .build();
    when(productionService.findConfirmedAwaitingSync(any(Pageable.class))).thenReturn(List.of(cycle));
    when(machineService.findById(UNKNOWN_MACHINE_ID)).thenReturn(Optional.empty());
    when(machineStatusService.findConfirmedDowntimeAwaitingSync(any(Pageable.class))).thenReturn(List.of());

    ErpSyncRun result = service.runSync();

    assertThat(result.getStatus()).isEqualTo(ErpSyncStatus.SUCCESS);
    assertThat(result.getCyclesWritten()).isZero();
    verify(erp, never()).insertCycle(any(Object[].class));
    verify(productionService).markCyclesAsSynced(List.of());
  }

  @Test
  void runSync_pushCyclesFailureReturnsPartialAndSkipsPauses() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenReturn(List.of());
    ProductionCycle cycle = ProductionCycle.builder()
        .id(UUID.randomUUID())
        .machineId(MACHINE_ID)
        .pulseTimestamp(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .sequence(1L)
        .state(RecordState.CONFIRMED)
        .build();
    when(productionService.findConfirmedAwaitingSync(any(Pageable.class))).thenReturn(List.of(cycle));
    when(machineService.findById(MACHINE_ID)).thenReturn(Optional.of(machine()));
    when(erp.insertCycle(any(Object[].class))).thenThrow(new DataAccessResourceFailureException("erp down"));

    ErpSyncRun result = service.runSync();

    assertThat(result.getStatus()).isEqualTo(ErpSyncStatus.PARTIAL);
    assertThat(result.getErrorMessage()).contains("push-cycles");
    verify(machineStatusService, never()).findConfirmedDowntimeAwaitingSync(any());
  }

  @Test
  void runSync_pushPausesWritesAndMarksAsSynced() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenReturn(List.of());
    when(productionService.findConfirmedAwaitingSync(any(Pageable.class))).thenReturn(List.of());
    UUID statusId = UUID.randomUUID();
    MachineStatus status = MachineStatus.builder()
        .id(statusId)
        .machineId(MACHINE_ID)
        .state(MachineState.AUTO_STOPPED)
        .reason("REASON")
        .message("MSG")
        .startTime(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .endTime(OffsetDateTime.parse("2026-05-28T14:01:00Z"))
        .consecutiveCountAtCreation(3)
        .recordState(RecordState.CONFIRMED)
        .build();
    when(machineStatusService.findConfirmedDowntimeAwaitingSync(any(Pageable.class)))
        .thenReturn(List.of(status));
    when(machineService.findById(MACHINE_ID)).thenReturn(Optional.of(machine()));

    ErpSyncRun result = service.runSync();

    assertThat(result.getStatus()).isEqualTo(ErpSyncStatus.SUCCESS);
    assertThat(result.getPausesWritten()).isEqualTo(1);
    verify(erp).insertPause(any(Object[].class), any(int[].class));
    verify(machineStatusService).markStatusesAsSynced(List.of(status));
  }

  @Test
  void runSync_pushPausesFailureReturnsPartial() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenReturn(List.of());
    when(productionService.findConfirmedAwaitingSync(any(Pageable.class))).thenReturn(List.of());
    MachineStatus status = MachineStatus.builder()
        .id(UUID.randomUUID())
        .machineId(MACHINE_ID)
        .state(MachineState.PAUSED)
        .startTime(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .build();
    when(machineStatusService.findConfirmedDowntimeAwaitingSync(any(Pageable.class)))
        .thenReturn(List.of(status));
    when(machineService.findById(MACHINE_ID)).thenReturn(Optional.of(machine()));
    when(erp.insertPause(any(Object[].class), any(int[].class)))
        .thenThrow(new DataAccessResourceFailureException("erp down"));

    ErpSyncRun result = service.runSync();

    assertThat(result.getStatus()).isEqualTo(ErpSyncStatus.PARTIAL);
    assertThat(result.getErrorMessage()).contains("push-pauses");
  }

  @Test
  void runSync_pushCyclesSkipsWhenNothingPending() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenReturn(List.of());
    when(productionService.findConfirmedAwaitingSync(any(Pageable.class))).thenReturn(List.of());
    when(machineStatusService.findConfirmedDowntimeAwaitingSync(any(Pageable.class))).thenReturn(List.of());

    ErpSyncRun result = service.runSync();

    assertThat(result.getStatus()).isEqualTo(ErpSyncStatus.SUCCESS);
    assertThat(result.getCyclesWritten()).isZero();
    assertThat(result.getPausesWritten()).isZero();
    verify(productionService, never()).markCyclesAsSynced(any());
    verify(machineStatusService, never()).markStatusesAsSynced(any());
    verify(erp, never()).insertCycle(any(Object[].class));
    verify(machineService, never()).findById(any());
    verify(machineService, never()).findActiveByCode(anyString());
  }

  @Test
  void runSync_pushPausesSkipsStatusesWhenMachineUnknown() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenReturn(List.of());
    when(productionService.findConfirmedAwaitingSync(any(Pageable.class))).thenReturn(List.of());
    MachineStatus status = MachineStatus.builder()
        .id(UUID.randomUUID())
        .machineId(UNKNOWN_MACHINE_ID)
        .state(MachineState.PAUSED)
        .startTime(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .build();
    when(machineStatusService.findConfirmedDowntimeAwaitingSync(any(Pageable.class)))
        .thenReturn(List.of(status));
    when(machineService.findById(UNKNOWN_MACHINE_ID)).thenReturn(Optional.empty());

    ErpSyncRun result = service.runSync();

    assertThat(result.getStatus()).isEqualTo(ErpSyncStatus.SUCCESS);
    assertThat(result.getPausesWritten()).isZero();
    verify(erp, never()).insertPause(any(Object[].class), any(int[].class));
    verify(machineStatusService).markStatusesAsSynced(List.of());
  }

  @Test
  void runSync_refreshOrderCache_skipsMachineLookupWhenCodeIsBlank() {
    when(erpRepositoryProvider.getIfAvailable()).thenReturn(erp);
    when(erp.findOpenOrders()).thenReturn(List.of(
        new ErpOrderRow("OS-1", " ", "SKU-1", 50, "OPEN", "{}")));
    when(orderCacheRepository.findByErpOrderId("OS-1")).thenReturn(Optional.empty());
    when(productionService.findConfirmedAwaitingSync(any(Pageable.class))).thenReturn(List.of());
    when(machineStatusService.findConfirmedDowntimeAwaitingSync(any(Pageable.class))).thenReturn(List.of());

    ErpSyncRun result = service.runSync();

    assertThat(result.getStatus()).isEqualTo(ErpSyncStatus.SUCCESS);
    verify(machineService, never()).findActiveByCode(eq(" "));
    verify(orderCacheRepository).save(any(ProductionOrderCache.class));
  }
}
