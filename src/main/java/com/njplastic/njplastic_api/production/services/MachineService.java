package com.njplastic.njplastic_api.production.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.exceptions.MachineAccessDeniedException;
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
   * Resolve an active machine by its Arduino code (RF01).
   *
   * @param code the short code provisioned on the microcontroller
   * @return the matching active machine, or empty if none exists or it is
   *         inactive
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
   * List the active machines visible to the authenticated user (RN02 to
   * RN04). MANAGER sees every active machine; OPERATOR and LEADER are
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
   * it (RN02 to RN04). Throws {@link UnknownMachineException} (404) when
   * the id is unknown and {@link MachineAccessDeniedException} (403) when
   * the machine exists but its sector is outside the principal scope.
   *
   * @param id        the machine UUID
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
}