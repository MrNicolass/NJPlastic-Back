package com.njplastic.njplastic_api.production.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.exceptions.MachineAccessDeniedException;
import com.njplastic.njplastic_api.production.exceptions.MachineAlreadyExistsException;
import com.njplastic.njplastic_api.production.exceptions.UnknownMachineException;
import com.njplastic.njplastic_api.production.repositories.MachineRepository;

import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link MachineRepository}. Other production aggregates read
 * machine data through this service rather than injecting the repository
 * directly, keeping a single point of access to the machine aggregate.
 */
@Service
@RequiredArgsConstructor
public class MachineService {

  private final MachineRepository machineRepository;

 /**
 * Resolve an active machine by its Arduino code.
 *
 * @param code the short code provisioned on the microcontroller
 * @return the matching active machine, or empty if none exists or it is
 * inactive
 */
  public Optional<Machine> findActiveByCode(String code) {
    return machineRepository.findByCodeAndActiveTrue(code);
  }

 /**
 * Resolve a machine by its UUID.
 *
 * @param id the machine UUID
 * @return the matching machine, or empty if none exists
 */
  public Optional<Machine> findById(UUID id) {
    return machineRepository.findById(id);
  }

 /**
 * List every active machine, used by the OFFLINE watchdog scan.
 *
 * @return all active machines
 */
  public List<Machine> findAllActive() {
    return machineRepository.findAllByActiveTrue();
  }

 /**
 * List the active machines visible to the authenticated user (to
 *). MANAGER sees every active machine; OPERATOR and LEADER are
 * scoped to their own sector. When a non-MANAGER principal has no
 * sector configured the response is empty rather than the full set.
 *
 * @param principal the authenticated user from the JWT
 * @return the active machines visible to the principal
 */
  public List<Machine> findAccessible(AuthenticatedUser principal) {
    if (principal.role() == UserRole.MANAGER) {
      return machineRepository.findAllByActiveTrue();
    }
    if (principal.sector() == null || principal.sector().isBlank()) {
      return List.of();
    }
    return machineRepository.findAllByActiveTrueAndSectorIgnoreCase(principal.sector());
  }

 /**
 * Resolve a machine by UUID and assert the principal is allowed to see
 * it (to). Throws {@link UnknownMachineException} (404) when
 * the id is unknown and {@link MachineAccessDeniedException} (403) when
 * the machine exists but its sector is outside the principal scope.
 *
 * @param id the machine UUID
 * @param principal the authenticated user from the JWT
 * @return the machine
 */
  public Machine requireAccessible(UUID id, AuthenticatedUser principal) {
    Machine machine = machineRepository.findById(id)
        .orElseThrow(() -> new UnknownMachineException("Machine not found: " + id));
    if (principal.role() == UserRole.MANAGER) {
      return machine;
    }
    String userSector = principal.sector();
    String machineSector = machine.getSector();
    if (userSector == null || machineSector == null || !userSector.equalsIgnoreCase(machineSector)) {
      throw new MachineAccessDeniedException("Access denied for machine " + id);
    }
    return machine;
  }

 /**
 * Persist a new machine. The supplied {@code code} must be globally unique
 * - the database enforces it, and this check produces a clean 409 instead
 * of a generic constraint violation. Created machines are always {@code active=true}.
 *
 * @param machine fully populated entity
 * @return the persisted entity
 * @throws MachineAlreadyExistsException if the code is taken
 */
  @Transactional
  public Machine create(Machine machine) {
    if (machineRepository.existsByCode(machine.getCode())) {
      throw new MachineAlreadyExistsException(machine.getCode());
    }
    machine.setActive(true);
    return machineRepository.save(machine);
  }

 /**
 * Apply administrative changes to an existing machine. {@code code} is
 * deliberately not editable - it identifies the machine on the MQTT payload
 * and changing it would orphan in-flight messages and historical
 * cycles.
 *
 * @param id machine UUID
 * @param updated entity carrying the fields to apply
 * @return the persisted entity after the update
 * @throws UnknownMachineException if no machine matches {@code id}
 */
  @Transactional
  public Machine update(UUID id, Machine updated) {
    Machine current = machineRepository.findById(id)
        .orElseThrow(() -> new UnknownMachineException("Machine not found: " + id));
    current.setDescription(updated.getDescription());
    current.setSector(updated.getSector());
    current.setStandardCycleMs(updated.getStandardCycleMs());
    current.setToleranceFactor(updated.getToleranceFactor());
    current.setConsecutivePausesToStop(updated.getConsecutivePausesToStop());
    current.setOfflineWindowMs(updated.getOfflineWindowMs());
    current.setActive(updated.isActive());
    return machineRepository.save(current);
  }

 /**
 * Mark the machine as inactive. Soft-delete preserves cycle history and
 * audit traceability.
 *
 * @param id machine UUID
 * @throws UnknownMachineException if no machine matches {@code id}
 */
  @Transactional
  public void softDelete(UUID id) {
    Machine current = machineRepository.findById(id)
        .orElseThrow(() -> new UnknownMachineException("Machine not found: " + id));
    current.setActive(false);
    current.setUpdatedAt(OffsetDateTime.now());
    machineRepository.save(current);
  }
}