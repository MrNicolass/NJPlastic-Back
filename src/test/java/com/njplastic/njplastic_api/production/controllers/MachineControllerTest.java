package com.njplastic.njplastic_api.production.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.production.dtos.EditStopMessageRequestDTO;
import com.njplastic.njplastic_api.production.dtos.MachineStatusEntryDTO;
import com.njplastic.njplastic_api.production.dtos.MachineSummaryDTO;
import com.njplastic.njplastic_api.production.dtos.OeeResultDTO;
import com.njplastic.njplastic_api.production.dtos.QualityRegistrationRequestDTO;
import com.njplastic.njplastic_api.production.dtos.RegisterPauseRequestDTO;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.entities.ProductionCycle;
import com.njplastic.njplastic_api.production.entities.QualityRecord;
import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.exceptions.PauseAlreadyClassifiedException;
import com.njplastic.njplastic_api.production.exceptions.StopMessageNotEditableException;
import com.njplastic.njplastic_api.production.exceptions.UnknownMachineException;
import com.njplastic.njplastic_api.production.services.MachineService;
import com.njplastic.njplastic_api.production.services.MachineStatusService;
import com.njplastic.njplastic_api.production.services.OeeService;
import com.njplastic.njplastic_api.production.services.ProductionDtoMapper;
import com.njplastic.njplastic_api.production.services.ProductionService;
import com.njplastic.njplastic_api.production.services.QualityService;

@WebMvcTest(MachineController.class)
@AutoConfigureMockMvc(addFilters = false)
class MachineControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @MockitoBean
  private MachineService machineService;

  @MockitoBean
  private MachineStatusService machineStatusService;

  @MockitoBean
  private ProductionService productionService;

  @MockitoBean
  private OeeService oeeService;

  @MockitoBean
  private QualityService qualityService;

  @MockitoBean
  private ProductionDtoMapper mapper;

  private static final UUID MACHINE_ID = UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d");
  private static final UUID USER_ID = UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c");

  @BeforeEach
  void setUp() {
    AuthenticatedUser principal = new AuthenticatedUser(USER_ID, "manager", UserRole.MANAGER, "INJECAO", "TURNO_A");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void listMachines_returnsAccessibleSummaries() throws Exception {
    Machine machine = Machine.builder().id(MACHINE_ID).code("MAQ-01").build();
    MachineSummaryDTO summary = MachineSummaryDTO.builder().id(MACHINE_ID).code("MAQ-01").build();
    when(machineService.findAccessible(any())).thenReturn(List.of(machine));
    when(machineStatusService.currentState(MACHINE_ID)).thenReturn(Optional.of(MachineState.RUNNING));
    when(mapper.toMachineSummary(eq(machine), eq(MachineState.RUNNING))).thenReturn(summary);

    mockMvc.perform(get("/machines"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("MAQ-01"));
  }

  @Test
  void getStatus_returnsTimeline() throws Exception {
    OffsetDateTime from = OffsetDateTime.parse("2026-05-28T06:00:00Z");
    OffsetDateTime to = OffsetDateTime.parse("2026-05-28T14:00:00Z");
    Machine machine = Machine.builder().id(MACHINE_ID).build();
    MachineStatus open = MachineStatus.builder().id(UUID.randomUUID()).machineId(MACHINE_ID).state(MachineState.RUNNING).startTime(from).build();
    MachineStatusEntryDTO entry = MachineStatusEntryDTO.builder().id(open.getId()).state(MachineState.RUNNING).build();

    when(machineService.requireAccessible(eq(MACHINE_ID), any())).thenReturn(machine);
    when(machineStatusService.findCurrentOpen(MACHINE_ID)).thenReturn(Optional.of(open));
    when(machineStatusService.findWindow(MACHINE_ID, from, to)).thenReturn(List.of(open));
    when(mapper.toStatusEntries(List.of(open))).thenReturn(List.of(entry));
    when(mapper.toStatusEntry(open)).thenReturn(entry);

    mockMvc.perform(get("/machines/{id}/status", MACHINE_ID)
            .param("from", "2026-05-28T06:00:00Z")
            .param("to", "2026-05-28T14:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.machineId").value(MACHINE_ID.toString()))
        .andExpect(jsonPath("$.currentState").value("RUNNING"))
        .andExpect(jsonPath("$.timeline.length()").value(1));
  }

  @Test
  void getStatus_404WhenMachineUnknown() throws Exception {
    when(machineService.requireAccessible(eq(MACHINE_ID), any()))
        .thenThrow(new UnknownMachineException("Machine not found: " + MACHINE_ID));

    mockMvc.perform(get("/machines/{id}/status", MACHINE_ID)
            .param("from", "2026-05-28T06:00:00Z")
            .param("to", "2026-05-28T14:00:00Z"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getCycles_returnsPageOfCycles() throws Exception {
    Machine machine = Machine.builder().id(MACHINE_ID).build();
    ProductionCycle cycle = ProductionCycle.builder().id(UUID.randomUUID()).machineId(MACHINE_ID).build();
    when(machineService.requireAccessible(eq(MACHINE_ID), any())).thenReturn(machine);
    when(productionService.findCycles(eq(MACHINE_ID), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(cycle)));
    when(mapper.toCycleResponse(any())).thenReturn(
        com.njplastic.njplastic_api.production.dtos.ProductionCycleResponseDTO.builder()
            .id(cycle.getId())
            .machineId(MACHINE_ID)
            .build());

    mockMvc.perform(get("/machines/{id}/cycles", MACHINE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1));
  }

  @Test
  void registerPause_classifiesPause() throws Exception {
    Machine machine = Machine.builder().id(MACHINE_ID).build();
    MachineStatus updated = MachineStatus.builder().id(UUID.randomUUID()).machineId(MACHINE_ID).state(MachineState.PAUSED).reason("Mold change").build();
    MachineStatusEntryDTO dto = MachineStatusEntryDTO.builder().id(updated.getId()).state(MachineState.PAUSED).reason("Mold change").build();
    when(machineService.requireAccessible(eq(MACHINE_ID), any())).thenReturn(machine);
    when(machineStatusService.classifyLastIsolatedPause(eq(MACHINE_ID), eq("Mold change"), any()))
        .thenReturn(updated);
    when(mapper.toStatusEntry(updated)).thenReturn(dto);

    String body = objectMapper.writeValueAsString(
        RegisterPauseRequestDTO.builder().reason("Mold change").build());

    mockMvc.perform(post("/machines/{id}/pauses", MACHINE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reason").value("Mold change"));
  }

  @Test
  void registerPause_returns409WhenNoPending() throws Exception {
    Machine machine = Machine.builder().id(MACHINE_ID).build();
    when(machineService.requireAccessible(eq(MACHINE_ID), any())).thenReturn(machine);
    when(machineStatusService.classifyLastIsolatedPause(eq(MACHINE_ID), any(), any()))
        .thenThrow(new PauseAlreadyClassifiedException("no pending"));

    String body = objectMapper.writeValueAsString(
        RegisterPauseRequestDTO.builder().reason("any").build());

    mockMvc.perform(post("/machines/{id}/pauses", MACHINE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void registerPause_returns400OnInvalidPayload() throws Exception {
    String body = objectMapper.writeValueAsString(
        RegisterPauseRequestDTO.builder().reason("").build());

    mockMvc.perform(post("/machines/{id}/pauses", MACHINE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void editStopMessage_updatesAndReturnsEntry() throws Exception {
    UUID stopId = UUID.randomUUID();
    Machine machine = Machine.builder().id(MACHINE_ID).build();
    MachineStatus updated = MachineStatus.builder().id(stopId).machineId(MACHINE_ID).state(MachineState.AUTO_STOPPED).message("new").build();
    MachineStatusEntryDTO dto = MachineStatusEntryDTO.builder().id(stopId).state(MachineState.AUTO_STOPPED).message("new").build();
    when(machineService.requireAccessible(eq(MACHINE_ID), any())).thenReturn(machine);
    when(machineStatusService.editAutoStopMessage(eq(MACHINE_ID), eq(stopId), eq("new"), any()))
        .thenReturn(updated);
    when(mapper.toStatusEntry(updated)).thenReturn(dto);

    String body = objectMapper.writeValueAsString(
        EditStopMessageRequestDTO.builder().message("new").build());

    mockMvc.perform(put("/machines/{id}/stops/{stopId}/message", MACHINE_ID, stopId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("new"));
  }

  @Test
  void editStopMessage_returns422WhenNotEditable() throws Exception {
    UUID stopId = UUID.randomUUID();
    Machine machine = Machine.builder().id(MACHINE_ID).build();
    when(machineService.requireAccessible(eq(MACHINE_ID), any())).thenReturn(machine);
    when(machineStatusService.editAutoStopMessage(eq(MACHINE_ID), eq(stopId), any(), any()))
        .thenThrow(new StopMessageNotEditableException("not AUTO_STOPPED"));

    String body = objectMapper.writeValueAsString(
        EditStopMessageRequestDTO.builder().message("x").build());

    mockMvc.perform(put("/machines/{id}/stops/{stopId}/message", MACHINE_ID, stopId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void getOee_returnsResult() throws Exception {
    OffsetDateTime from = OffsetDateTime.parse("2026-05-28T06:00:00Z");
    OffsetDateTime to = OffsetDateTime.parse("2026-05-28T14:00:00Z");
    Machine machine = Machine.builder().id(MACHINE_ID).build();
    OeeResultDTO result = OeeResultDTO.builder()
        .machineId(MACHINE_ID)
        .periodStart(from)
        .periodEnd(to)
        .availability(0.9)
        .performance(0.8)
        .quality(0.95)
        .oee(0.684)
        .partial(false)
        .build();
    when(machineService.requireAccessible(eq(MACHINE_ID), any())).thenReturn(machine);
    when(oeeService.calculate(MACHINE_ID, from, to)).thenReturn(result);

    mockMvc.perform(get("/machines/{id}/oee", MACHINE_ID)
            .param("from", "2026-05-28T06:00:00Z")
            .param("to", "2026-05-28T14:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.partial").value(false))
        .andExpect(jsonPath("$.quality").value(0.95));
  }

  @Test
  void registerQuality_savesQualityAndReturnsRecord() throws Exception {
    Machine machine = Machine.builder().id(MACHINE_ID).build();
    QualityRecord record = QualityRecord.builder()
        .id(UUID.randomUUID())
        .machineId(MACHINE_ID)
        .goodCount(950)
        .totalCount(1000)
        .build();
    when(machineService.requireAccessible(eq(MACHINE_ID), any())).thenReturn(machine);
    when(qualityService.registerQuality(any(QualityRegistrationRequestDTO.class), any())).thenReturn(record);

    String body = objectMapper.writeValueAsString(
        QualityRegistrationRequestDTO.builder()
            .machineId(MACHINE_ID)
            .periodStart(OffsetDateTime.parse("2026-05-28T06:00:00Z"))
            .periodEnd(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
            .goodCount(950)
            .totalCount(1000)
            .build());

    mockMvc.perform(post("/machines/{id}/quality", MACHINE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.goodCount").value(950));
  }
}
