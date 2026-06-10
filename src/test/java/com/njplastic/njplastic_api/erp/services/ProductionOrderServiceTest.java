package com.njplastic.njplastic_api.erp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.erp.dtos.ProductionOrderSummaryDTO;
import com.njplastic.njplastic_api.erp.entities.ProductionOrderCache;
import com.njplastic.njplastic_api.erp.repositories.ProductionOrderCacheRepository;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.services.MachineService;

@ExtendWith(MockitoExtension.class)
class ProductionOrderServiceTest {

  @Mock
  private ProductionOrderCacheRepository repository;

  @Mock
  private MachineService machineService;

  @InjectMocks
  private ProductionOrderService service;

  private static final UUID MAQ_01 = UUID.fromString("a0000000-0001-4001-8001-000000000001");
  private static final UUID MAQ_02 = UUID.fromString("a0000000-0001-4001-8001-000000000002");
  private static final AuthenticatedUser MANAGER = new AuthenticatedUser(
      UUID.fromString("11111111-1111-4111-8111-111111111111"), "manager", UserRole.MANAGER, null, null);
  private static final AuthenticatedUser LEADER = new AuthenticatedUser(
      UUID.fromString("22222222-2222-4222-8222-222222222222"), "leader", UserRole.LEADER, "INJECAO", "TURNO_A");

  @Test
  void findPaged_asManagerDelegatesWithoutMachineScope() {
    Pageable pageable = PageRequest.of(0, 10);
    ProductionOrderCache row = ProductionOrderCache.builder().erpOrderId("OP-1").build();
    Page<ProductionOrderCache> page = new PageImpl<>(List.of(row));
    when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    Page<ProductionOrderCache> result = service.findPaged(null, null, null, null, MANAGER, pageable);

    assertThat(result.getContent()).containsExactly(row);
  }

  @Test
  void findPaged_asLeaderScopesByAccessibleMachines() {
    Pageable pageable = PageRequest.of(0, 5);
    Machine machine = Machine.builder().id(MAQ_01).code("MAQ-01").build();
    when(machineService.findAccessible(LEADER)).thenReturn(List.of(machine));
    when(repository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));

    Page<ProductionOrderCache> result = service.findPaged("OPEN", null,
        OffsetDateTime.now().minusDays(1), OffsetDateTime.now(), LEADER, pageable);

    assertThat(result.getContent()).isEmpty();
  }

  @Test
  void summarize_asManagerCountsAllCategories() {
    when(repository.findAll()).thenReturn(List.of(
        ProductionOrderCache.builder().machineId(MAQ_01).status("IN_PRODUCTION").build(),
        ProductionOrderCache.builder().machineId(MAQ_02).status("in_production").build(),
        ProductionOrderCache.builder().machineId(null).status("OPEN").build(),
        ProductionOrderCache.builder().machineId(MAQ_01).status("OVERDUE").build(),
        ProductionOrderCache.builder().machineId(MAQ_02).status("COMPLETED").build()));

    ProductionOrderSummaryDTO summary = service.summarize(MANAGER);

    assertThat(summary.getInProd()).isEqualTo(2);
    assertThat(summary.getQueued()).isEqualTo(1);
    assertThat(summary.getOverdue()).isEqualTo(1);
    assertThat(summary.getCompleted()).isEqualTo(1);
  }

  @Test
  void summarize_asLeaderSkipsOrdersOutsideAccessibleMachines() {
    when(machineService.findAccessible(LEADER)).thenReturn(List.of(
        Machine.builder().id(MAQ_01).code("MAQ-01").build()));
    when(repository.findAll()).thenReturn(List.of(
        ProductionOrderCache.builder().machineId(MAQ_01).status("IN_PRODUCTION").build(),
        ProductionOrderCache.builder().machineId(MAQ_02).status("IN_PRODUCTION").build(),
        ProductionOrderCache.builder().machineId(null).status("OPEN").build()));

    ProductionOrderSummaryDTO summary = service.summarize(LEADER);

    assertThat(summary.getInProd()).isEqualTo(1);
    assertThat(summary.getQueued()).isZero();
    assertThat(summary.getOverdue()).isZero();
    assertThat(summary.getCompleted()).isZero();
  }

  @Test
  void summarize_treatsRowWithUnknownStatusAsInProdWhenBoundToMachine() {
    when(repository.findAll()).thenReturn(List.of(
        ProductionOrderCache.builder().machineId(MAQ_01).status("WAITING_MATERIAL").build(),
        ProductionOrderCache.builder().machineId(MAQ_02).status(null).build()));

    ProductionOrderSummaryDTO summary = service.summarize(MANAGER);

    assertThat(summary.getInProd()).isEqualTo(2);
    assertThat(summary.getQueued()).isZero();
  }
}
