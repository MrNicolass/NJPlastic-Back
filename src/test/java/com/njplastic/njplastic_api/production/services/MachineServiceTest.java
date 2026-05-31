package com.njplastic.njplastic_api.production.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.exceptions.MachineAccessDeniedException;
import com.njplastic.njplastic_api.production.exceptions.UnknownMachineException;
import com.njplastic.njplastic_api.production.repositories.MachineRepository;

@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

  @Mock
  private MachineRepository machineRepository;

  @InjectMocks
  private MachineService machineService;

  private AuthenticatedUser principal(UserRole role, String sector) {
    return new AuthenticatedUser(UUID.randomUUID(), "user", role, sector, "TURNO_A");
  }

  @Test
  void findActiveByCode_delegatesToRepository() {
    Machine machine = Machine.builder().id(UUID.randomUUID()).code("MAQ-01").build();
    when(machineRepository.findByCodeAndActiveTrue("MAQ-01")).thenReturn(Optional.of(machine));

    assertThat(machineService.findActiveByCode("MAQ-01")).containsSame(machine);
  }

  @Test
  void findActiveByCode_returnsEmptyWhenRepositoryEmpty() {
    when(machineRepository.findByCodeAndActiveTrue("ghost")).thenReturn(Optional.empty());

    assertThat(machineService.findActiveByCode("ghost")).isEmpty();
  }

  @Test
  void findById_delegatesToRepository() {
    UUID id = UUID.randomUUID();
    Machine machine = Machine.builder().id(id).build();
    when(machineRepository.findById(id)).thenReturn(Optional.of(machine));

    assertThat(machineService.findById(id)).containsSame(machine);
  }

  @Test
  void findAllActive_delegatesToRepository() {
    Machine machine = Machine.builder().id(UUID.randomUUID()).build();
    when(machineRepository.findAllByActiveTrue()).thenReturn(List.of(machine));

    assertThat(machineService.findAllActive()).containsExactly(machine);
  }

  @Test
  void findAccessible_managerSeesEveryActiveMachine() {
    Machine machine = Machine.builder().id(UUID.randomUUID()).build();
    when(machineRepository.findAllByActiveTrue()).thenReturn(List.of(machine));

    assertThat(machineService.findAccessible(principal(UserRole.MANAGER, null)))
        .containsExactly(machine);
  }

  @Test
  void findAccessible_leaderScopedBySector() {
    Machine machine = Machine.builder().id(UUID.randomUUID()).sector("INJECAO").build();
    when(machineRepository.findAllByActiveTrueAndSectorIgnoreCase("INJECAO"))
        .thenReturn(List.of(machine));

    assertThat(machineService.findAccessible(principal(UserRole.LEADER, "INJECAO")))
        .containsExactly(machine);
  }

  @Test
  void findAccessible_operatorWithoutSectorReturnsEmpty() {
    assertThat(machineService.findAccessible(principal(UserRole.OPERATOR, null))).isEmpty();
    assertThat(machineService.findAccessible(principal(UserRole.OPERATOR, "  "))).isEmpty();
  }

  @Test
  void requireAccessible_throwsUnknownWhenMissing() {
    UUID id = UUID.randomUUID();
    when(machineRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> machineService.requireAccessible(id, principal(UserRole.MANAGER, null)))
        .isInstanceOf(UnknownMachineException.class);
  }

  @Test
  void requireAccessible_managerAlwaysWins() {
    UUID id = UUID.randomUUID();
    Machine machine = Machine.builder().id(id).sector("INJECAO").build();
    when(machineRepository.findById(id)).thenReturn(Optional.of(machine));

    assertThat(machineService.requireAccessible(id, principal(UserRole.MANAGER, "OUTRO"))).isSameAs(machine);
  }

  @Test
  void requireAccessible_throwsAccessDeniedWhenSectorMismatch() {
    UUID id = UUID.randomUUID();
    Machine machine = Machine.builder().id(id).sector("INJECAO").build();
    when(machineRepository.findById(id)).thenReturn(Optional.of(machine));

    assertThatThrownBy(() -> machineService.requireAccessible(id, principal(UserRole.OPERATOR, "MONTAGEM")))
        .isInstanceOf(MachineAccessDeniedException.class);
  }

  @Test
  void requireAccessible_returnsMachineWhenSectorMatchesIgnoringCase() {
    UUID id = UUID.randomUUID();
    Machine machine = Machine.builder().id(id).sector("INJECAO").build();
    when(machineRepository.findById(id)).thenReturn(Optional.of(machine));

    assertThat(machineService.requireAccessible(id, principal(UserRole.LEADER, "injecao"))).isSameAs(machine);
  }

  @Test
  void requireAccessible_throwsWhenPrincipalSectorIsNull() {
    UUID id = UUID.randomUUID();
    Machine machine = Machine.builder().id(id).sector("INJECAO").build();
    when(machineRepository.findById(id)).thenReturn(Optional.of(machine));

    assertThatThrownBy(() -> machineService.requireAccessible(id, principal(UserRole.OPERATOR, null)))
        .isInstanceOf(MachineAccessDeniedException.class);
  }
}
