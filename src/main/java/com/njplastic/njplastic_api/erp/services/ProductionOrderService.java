package com.njplastic.njplastic_api.erp.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.erp.dtos.ProductionOrderSummaryDTO;
import com.njplastic.njplastic_api.erp.entities.ProductionOrderCache;
import com.njplastic.njplastic_api.erp.repositories.ProductionOrderCacheRepository;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.services.MachineService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link ProductionOrderCacheRepository} (EP-BE-08 sub-task 3).
 * Reads are scoped per RN02-RN04: MANAGER sees every cached order; OPERATOR
 * and LEADER only see orders bound to machines in their sector (resolved via
 * {@link MachineService#findAccessible}).
 */
@Service
@RequiredArgsConstructor
public class ProductionOrderService {

  private static final String STATUS_OPEN = "OPEN";
  private static final String STATUS_COMPLETED = "COMPLETED";
  private static final String STATUS_OVERDUE = "OVERDUE";

  private final ProductionOrderCacheRepository repository;
  private final MachineService machineService;

  /**
   * Paginated read of cached production orders, optionally filtered.
   *
   * @param status    optional ERP status (exact match, case-insensitive)
   * @param machineId optional machine filter; ignored when null
   * @param from      optional inclusive lower bound on lastSyncAt
   * @param to        optional inclusive upper bound on lastSyncAt
   * @param principal authenticated user used to scope by sector
   * @param pageable  paging/sort
   * @return page of cached orders
   */
  public Page<ProductionOrderCache> findPaged(String status, UUID machineId, OffsetDateTime from, OffsetDateTime to,
      AuthenticatedUser principal, Pageable pageable) {
    Set<UUID> allowedMachineIds = resolveAccessibleMachineIds(principal);
    Specification<ProductionOrderCache> spec = (root, query, cb) -> {
      Predicate predicate = cb.conjunction();
      if (allowedMachineIds != null) {
        if (allowedMachineIds.isEmpty()) {
          return cb.disjunction();
        }
        predicate = cb.and(predicate, root.get("machineId").in(allowedMachineIds));
      }
      if (status != null && !status.isBlank()) {
        predicate = cb.and(predicate, cb.equal(cb.upper(root.get("status")), status.toUpperCase(Locale.ROOT)));
      }
      if (machineId != null) {
        predicate = cb.and(predicate, cb.equal(root.get("machineId"), machineId));
      }
      if (from != null) {
        predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("lastSyncAt"), from));
      }
      if (to != null) {
        predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("lastSyncAt"), to));
      }
      return predicate;
    };
    return repository.findAll(spec, pageable);
  }

  /**
   * Build the four-counter KPI snapshot consumed by the OS screen header.
   * Counts respect the principal scope so OPERATOR/LEADER never see numbers
   * outside their sector.
   *
   * @param principal authenticated user used to scope by sector
   * @return populated summary
   */
  public ProductionOrderSummaryDTO summarize(AuthenticatedUser principal) {
    Set<UUID> allowedMachineIds = resolveAccessibleMachineIds(principal);
    List<ProductionOrderCache> rows = repository.findAll();
    long inProd = 0;
    long queued = 0;
    long overdue = 0;
    long completed = 0;
    for (ProductionOrderCache row : rows) {
      if (allowedMachineIds != null && (row.getMachineId() == null || !allowedMachineIds.contains(row.getMachineId()))) {
        continue;
      }
      String upperStatus = row.getStatus() == null ? "" : row.getStatus().toUpperCase(Locale.ROOT);
      if (STATUS_COMPLETED.equals(upperStatus)) {
        completed++;
        continue;
      }
      if (STATUS_OVERDUE.equals(upperStatus)) {
        overdue++;
        continue;
      }
      if (row.getMachineId() == null || STATUS_OPEN.equals(upperStatus)) {
        queued++;
        continue;
      }
      inProd++;
    }
    return ProductionOrderSummaryDTO.builder()
        .inProd(inProd)
        .queued(queued)
        .overdue(overdue)
        .completed(completed)
        .build();
  }

  private Set<UUID> resolveAccessibleMachineIds(AuthenticatedUser principal) {
    if (principal == null || principal.role() == UserRole.MANAGER) {
      return null;
    }
    return machineService.findAccessible(principal).stream()
        .map(Machine::getId)
        .collect(java.util.stream.Collectors.toSet());
  }
}
