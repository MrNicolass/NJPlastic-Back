package com.njplastic.njplastic_api.production.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.repositories.MachineRepository;

@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

  @Mock
  private MachineRepository machineRepository;

  @InjectMocks
  private MachineService machineService;

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
}
