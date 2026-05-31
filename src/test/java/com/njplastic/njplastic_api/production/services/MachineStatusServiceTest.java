package com.njplastic.njplastic_api.production.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecordState;
import com.njplastic.njplastic_api.production.exceptions.MachineStatusPersistenceException;
import com.njplastic.njplastic_api.production.repositories.MachineStatusRepository;

@ExtendWith(MockitoExtension.class)
class MachineStatusServiceTest {

  @Mock
  private MachineStatusRepository machineStatusRepository;

  @InjectMocks
  private MachineStatusService service;

  private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-05-28T14:00:00Z");
  private static final OffsetDateTime T1 = OffsetDateTime.parse("2026-05-28T14:01:00Z");
  private static final UUID MACHINE_ID = UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d");

  private Machine machine() {
    return Machine.builder().id(MACHINE_ID).code("MAQ-01").build();
  }

  private MachineStatus open(MachineState state) {
    return MachineStatus.builder()
        .id(UUID.randomUUID())
        .machineId(MACHINE_ID)
        .state(state)
        .startTime(T0)
        .recordState(RecordState.CONFIRMED)
        .build();
  }

  @Test
  void findCurrentOpen_delegatesToRepository() {
    MachineStatus open = open(MachineState.RUNNING);
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.of(open));

    assertThat(service.findCurrentOpen(MACHINE_ID)).containsSame(open);
  }

  @Test
  void currentState_mapsToOpenState() {
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.of(open(MachineState.AUTO_STOPPED)));

    assertThat(service.currentState(MACHINE_ID)).contains(MachineState.AUTO_STOPPED);
  }

  @Test
  void currentState_returnsEmptyWhenNoRecord() {
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.empty());

    assertThat(service.currentState(MACHINE_ID)).isEmpty();
  }

  @Test
  void resumeRunning_noOpWhenAlreadyRunning() {
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.of(open(MachineState.RUNNING)));

    service.resumeRunning(machine(), T1);

    verify(machineStatusRepository, never()).save(any());
  }

  @Test
  void resumeRunning_closesOpenAndOpensRunning() {
    MachineStatus open = open(MachineState.AUTO_STOPPED);
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.of(open));

    service.resumeRunning(machine(), T1);

    ArgumentCaptor<MachineStatus> captor = ArgumentCaptor.forClass(MachineStatus.class);
    verify(machineStatusRepository, times(2)).save(captor.capture());
    List<MachineStatus> saved = captor.getAllValues();
    assertThat(saved.get(0).getEndTime()).isEqualTo(T1);
    assertThat(saved.get(1).getState()).isEqualTo(MachineState.RUNNING);
    assertThat(saved.get(1).getStartTime()).isEqualTo(T1);
    assertThat(saved.get(1).getEndTime()).isNull();
  }

  @Test
  void resumeRunning_openRunningWhenNoCurrent() {
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.empty());

    service.resumeRunning(machine(), T1);

    ArgumentCaptor<MachineStatus> captor = ArgumentCaptor.forClass(MachineStatus.class);
    verify(machineStatusRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getState()).isEqualTo(MachineState.RUNNING);
  }

  @Test
  void resumeRunning_wrapsDataAccessException() {
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenThrow(new DataAccessResourceFailureException("db down"));

    assertThatThrownBy(() -> service.resumeRunning(machine(), T1))
        .isInstanceOf(MachineStatusPersistenceException.class)
        .hasMessageContaining("RUNNING");
  }

  @Test
  void recordIsolatedPause_closesOpenSavesPauseOpensRunning() {
    MachineStatus open = open(MachineState.RUNNING);
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.of(open));

    service.recordIsolatedPause(machine(), T0, T1, 1);

    ArgumentCaptor<MachineStatus> captor = ArgumentCaptor.forClass(MachineStatus.class);
    verify(machineStatusRepository, times(3)).save(captor.capture());
    List<MachineStatus> saved = captor.getAllValues();
    assertThat(saved.get(0).getEndTime()).isEqualTo(T0);
    assertThat(saved.get(1).getState()).isEqualTo(MachineState.PAUSED);
    assertThat(saved.get(1).getEndTime()).isEqualTo(T1);
    assertThat(saved.get(1).getConsecutiveCountAtCreation()).isEqualTo(1);
    assertThat(saved.get(2).getState()).isEqualTo(MachineState.RUNNING);
    assertThat(saved.get(2).getStartTime()).isEqualTo(T1);
  }

  @Test
  void recordIsolatedPause_wrapsDataAccessException() {
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenThrow(new DataAccessResourceFailureException("db down"));

    assertThatThrownBy(() -> service.recordIsolatedPause(machine(), T0, T1, 1))
        .isInstanceOf(MachineStatusPersistenceException.class)
        .hasMessageContaining("isolated pause");
  }

  @Test
  void recordAutoStop_closesOpenSavesPauseOpensStopped() {
    MachineStatus open = open(MachineState.RUNNING);
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.of(open));

    service.recordAutoStop(machine(), T0, T1, 3, "PARADA_AUTOMATICA");

    ArgumentCaptor<MachineStatus> captor = ArgumentCaptor.forClass(MachineStatus.class);
    verify(machineStatusRepository, times(3)).save(captor.capture());
    List<MachineStatus> saved = captor.getAllValues();
    assertThat(saved.get(1).getState()).isEqualTo(MachineState.PAUSED);
    MachineStatus opened = saved.get(2);
    assertThat(opened.getState()).isEqualTo(MachineState.AUTO_STOPPED);
    assertThat(opened.getMessage()).isEqualTo("PARADA_AUTOMATICA");
    assertThat(opened.getConsecutiveCountAtCreation()).isEqualTo(3);
    assertThat(opened.getEndTime()).isNull();
  }

  @Test
  void recordAutoStop_wrapsDataAccessException() {
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenThrow(new DataAccessResourceFailureException("db down"));

    assertThatThrownBy(() -> service.recordAutoStop(machine(), T0, T1, 3, "msg"))
        .isInstanceOf(MachineStatusPersistenceException.class)
        .hasMessageContaining("auto-stop");
  }

  @Test
  void recordPauseUnderStop_persistsClosedPauseOnly() {
    service.recordPauseUnderStop(machine(), T0, T1, 5);

    ArgumentCaptor<MachineStatus> captor = ArgumentCaptor.forClass(MachineStatus.class);
    verify(machineStatusRepository, times(1)).save(captor.capture());
    MachineStatus saved = captor.getValue();
    assertThat(saved.getState()).isEqualTo(MachineState.PAUSED);
    assertThat(saved.getStartTime()).isEqualTo(T0);
    assertThat(saved.getEndTime()).isEqualTo(T1);
    assertThat(saved.getConsecutiveCountAtCreation()).isEqualTo(5);
    assertThat(saved.getRecordState()).isEqualTo(RecordState.CONFIRMED);
  }

  @Test
  void recordPauseUnderStop_wrapsDataAccessException() {
    when(machineStatusRepository.save(any(MachineStatus.class)))
        .thenThrow(new DataAccessResourceFailureException("db down"));

    assertThatThrownBy(() -> service.recordPauseUnderStop(machine(), T0, T1, 5))
        .isInstanceOf(MachineStatusPersistenceException.class)
        .hasMessageContaining("pause under stop");
  }

  @Test
  void markOffline_noOpWhenAlreadyOffline() {
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.of(open(MachineState.OFFLINE)));

    service.markOffline(machine(), T1);

    verify(machineStatusRepository, never()).save(any());
  }

  @Test
  void markOffline_closesOpenAndOpensOfflineWhenRunning() {
    MachineStatus open = open(MachineState.RUNNING);
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.of(open));

    service.markOffline(machine(), T1);

    ArgumentCaptor<MachineStatus> captor = ArgumentCaptor.forClass(MachineStatus.class);
    verify(machineStatusRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues().get(0).getEndTime()).isEqualTo(T1);
    assertThat(captor.getAllValues().get(1).getState()).isEqualTo(MachineState.OFFLINE);
  }

  @Test
  void markOffline_opensOfflineWhenNoCurrent() {
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenReturn(Optional.empty());

    service.markOffline(machine(), T1);

    ArgumentCaptor<MachineStatus> captor = ArgumentCaptor.forClass(MachineStatus.class);
    verify(machineStatusRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getState()).isEqualTo(MachineState.OFFLINE);
  }

  @Test
  void markOffline_wrapsDataAccessException() {
    when(machineStatusRepository.findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(MACHINE_ID))
        .thenThrow(new DataAccessResourceFailureException("db down"));

    assertThatThrownBy(() -> service.markOffline(machine(), T1))
        .isInstanceOf(MachineStatusPersistenceException.class)
        .hasMessageContaining("OFFLINE");
  }

  @Test
  void findDowntimeOverlapping_delegatesWithDowntimeStates() {
    MachineStatus record = open(MachineState.PAUSED);
    when(machineStatusRepository.findOverlapping(
        MACHINE_ID,
        List.of(MachineState.PAUSED, MachineState.AUTO_STOPPED, MachineState.OFFLINE),
        T0, T1)).thenReturn(List.of(record));

    assertThat(service.findDowntimeOverlapping(MACHINE_ID, T0, T1)).containsExactly(record);
  }

  @Test
  void findWindow_delegatesToRepository() {
    MachineStatus record = open(MachineState.RUNNING);
    when(machineStatusRepository.findWindow(MACHINE_ID, T0, T1)).thenReturn(List.of(record));

    assertThat(service.findWindow(MACHINE_ID, T0, T1)).containsExactly(record);
  }

  @Test
  void findWindowByStates_delegatesToRepository() {
    MachineStatus record = open(MachineState.PAUSED);
    when(machineStatusRepository.findWindowByStates(MACHINE_ID,
        List.of(MachineState.PAUSED), T0, T1)).thenReturn(List.of(record));

    assertThat(service.findWindowByStates(MACHINE_ID, List.of(MachineState.PAUSED), T0, T1))
        .containsExactly(record);
  }

  @Test
  void classifyLastIsolatedPause_attachesReasonAndAuthor() {
    UUID authorId = UUID.randomUUID();
    MachineStatus target = open(MachineState.PAUSED);
    when(machineStatusRepository
        .findTopByMachineIdAndStateAndReasonIsNullOrderByStartTimeDesc(MACHINE_ID, MachineState.PAUSED))
        .thenReturn(Optional.of(target));
    when(machineStatusRepository.save(any(MachineStatus.class))).thenAnswer(inv -> inv.getArgument(0));

    MachineStatus updated = service.classifyLastIsolatedPause(MACHINE_ID, "Mold change", authorId);

    assertThat(updated.getReason()).isEqualTo("Mold change");
    assertThat(updated.getReasonAuthorId()).isEqualTo(authorId);
    verify(machineStatusRepository).save(target);
  }

  @Test
  void classifyLastIsolatedPause_throwsWhenNoPending() {
    when(machineStatusRepository
        .findTopByMachineIdAndStateAndReasonIsNullOrderByStartTimeDesc(MACHINE_ID, MachineState.PAUSED))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.classifyLastIsolatedPause(MACHINE_ID, "x", UUID.randomUUID()))
        .isInstanceOf(com.njplastic.njplastic_api.production.exceptions.PauseAlreadyClassifiedException.class);
  }

  @Test
  void classifyLastIsolatedPause_wrapsDataAccessException() {
    MachineStatus target = open(MachineState.PAUSED);
    when(machineStatusRepository
        .findTopByMachineIdAndStateAndReasonIsNullOrderByStartTimeDesc(MACHINE_ID, MachineState.PAUSED))
        .thenReturn(Optional.of(target));
    when(machineStatusRepository.save(any(MachineStatus.class)))
        .thenThrow(new DataAccessResourceFailureException("db down"));

    assertThatThrownBy(() -> service.classifyLastIsolatedPause(MACHINE_ID, "x", UUID.randomUUID()))
        .isInstanceOf(MachineStatusPersistenceException.class)
        .hasMessageContaining("classify isolated pause");
  }

  @Test
  void editAutoStopMessage_updatesMessageAndAuthor() {
    UUID stopId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    MachineStatus stop = MachineStatus.builder()
        .id(stopId)
        .machineId(MACHINE_ID)
        .state(MachineState.AUTO_STOPPED)
        .startTime(T0)
        .recordState(RecordState.CONFIRMED)
        .build();
    when(machineStatusRepository.findById(stopId)).thenReturn(Optional.of(stop));
    when(machineStatusRepository.save(any(MachineStatus.class))).thenAnswer(inv -> inv.getArgument(0));

    MachineStatus updated = service.editAutoStopMessage(MACHINE_ID, stopId, "new message", authorId);

    assertThat(updated.getMessage()).isEqualTo("new message");
    assertThat(updated.getReasonAuthorId()).isEqualTo(authorId);
  }

  @Test
  void editAutoStopMessage_throwsStopNotFoundWhenMissing() {
    UUID stopId = UUID.randomUUID();
    when(machineStatusRepository.findById(stopId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.editAutoStopMessage(MACHINE_ID, stopId, "msg", UUID.randomUUID()))
        .isInstanceOf(com.njplastic.njplastic_api.production.exceptions.StopNotFoundException.class);
  }

  @Test
  void editAutoStopMessage_throwsStopNotFoundWhenMachineMismatch() {
    UUID stopId = UUID.randomUUID();
    MachineStatus stop = MachineStatus.builder()
        .id(stopId)
        .machineId(UUID.randomUUID())
        .state(MachineState.AUTO_STOPPED)
        .startTime(T0)
        .build();
    when(machineStatusRepository.findById(stopId)).thenReturn(Optional.of(stop));

    assertThatThrownBy(() -> service.editAutoStopMessage(MACHINE_ID, stopId, "msg", UUID.randomUUID()))
        .isInstanceOf(com.njplastic.njplastic_api.production.exceptions.StopNotFoundException.class);
  }

  @Test
  void editAutoStopMessage_throwsWhenNotAutoStopped() {
    UUID stopId = UUID.randomUUID();
    MachineStatus stop = MachineStatus.builder()
        .id(stopId)
        .machineId(MACHINE_ID)
        .state(MachineState.PAUSED)
        .startTime(T0)
        .build();
    when(machineStatusRepository.findById(stopId)).thenReturn(Optional.of(stop));

    assertThatThrownBy(() -> service.editAutoStopMessage(MACHINE_ID, stopId, "msg", UUID.randomUUID()))
        .isInstanceOf(com.njplastic.njplastic_api.production.exceptions.StopMessageNotEditableException.class);
  }

  @Test
  void editAutoStopMessage_wrapsDataAccessException() {
    UUID stopId = UUID.randomUUID();
    MachineStatus stop = MachineStatus.builder()
        .id(stopId)
        .machineId(MACHINE_ID)
        .state(MachineState.AUTO_STOPPED)
        .startTime(T0)
        .build();
    when(machineStatusRepository.findById(stopId)).thenReturn(Optional.of(stop));
    when(machineStatusRepository.save(any(MachineStatus.class)))
        .thenThrow(new DataAccessResourceFailureException("db down"));

    assertThatThrownBy(() -> service.editAutoStopMessage(MACHINE_ID, stopId, "msg", UUID.randomUUID()))
        .isInstanceOf(MachineStatusPersistenceException.class)
        .hasMessageContaining("auto-stop message");
  }
}
