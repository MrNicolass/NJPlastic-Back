package com.njplastic.njplastic_api.production.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.repositories.MachineRepository;

import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link MachineRepository}. Other production aggregates read
 * machine data through this service rather than injecting the repository
 * directly,
 * keeping a single point of access to the machine aggregate.
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
}