package com.njplastic.njplastic_api.reports.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
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
import com.njplastic.njplastic_api.reports.dtos.ReportScheduleRequestDTO;
import com.njplastic.njplastic_api.reports.entities.ReportHistory;
import com.njplastic.njplastic_api.reports.entities.ReportSchedule;
import com.njplastic.njplastic_api.reports.enums.ReportFormat;
import com.njplastic.njplastic_api.reports.enums.ReportType;
import com.njplastic.njplastic_api.reports.services.ReportScheduleService;

@WebMvcTest(ReportsScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportsScheduleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @MockitoBean
  private ReportScheduleService service;

  private static final UUID PRINCIPAL_ID = UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c");

  @BeforeEach
  void setUp() {
    AuthenticatedUser principal = new AuthenticatedUser(
        PRINCIPAL_ID, "manager", UserRole.MANAGER, "INJECAO", "TURNO_A");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ReportHistory sampleHistory() {
    return ReportHistory.builder()
        .id(UUID.fromString("8c7b6a5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d"))
        .type(ReportType.SHIFT)
        .format(ReportFormat.CSV)
        .generatedAt(OffsetDateTime.parse("2026-06-01T07:00:00Z"))
        .path("./reports/2026/06/01/shift_07-00.csv")
        .sizeBytes(1024L)
        .build();
  }

  private ReportSchedule sampleSchedule() {
    return ReportSchedule.builder()
        .id(UUID.fromString("7b6a5c4d-3e2f-1a0b-9c8d-7e6f5a4b3c2d"))
        .type(ReportType.SHIFT)
        .cron("0 0 7 * * MON-FRI")
        .deliveryEmail("manager@njplastic.com")
        .format(ReportFormat.CSV)
        .active(true)
        .createdBy(PRINCIPAL_ID)
        .createdAt(OffsetDateTime.parse("2026-06-01T08:30:00Z"))
        .build();
  }

  @Test
  void history_returnsPage() throws Exception {
    when(service.findHistory(any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(sampleHistory())));

    mockMvc.perform(get("/reports/history"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].type").value("SHIFT"));
  }

  @Test
  void history_forwardsAllFilters() throws Exception {
    when(service.findHistory(eq(ReportType.SHIFT), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/reports/history")
            .param("type", "SHIFT")
            .param("from", "2026-06-01T00:00:00Z")
            .param("to", "2026-06-01T23:59:59Z"))
        .andExpect(status().isOk());

    verify(service).findHistory(eq(ReportType.SHIFT), any(), any(), any(Pageable.class));
  }

  @Test
  void createSchedule_returns201WhenValid() throws Exception {
    ReportSchedule saved = sampleSchedule();
    when(service.create(any(ReportSchedule.class))).thenReturn(saved);

    String body = objectMapper.writeValueAsString(ReportScheduleRequestDTO.builder()
        .type(ReportType.SHIFT)
        .cron("0 0 7 * * MON-FRI")
        .deliveryEmail("manager@njplastic.com")
        .format(ReportFormat.CSV)
        .build());

    mockMvc.perform(post("/reports/schedule")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("SHIFT"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void createSchedule_returns400OnInvalidPayload() throws Exception {
    String body = objectMapper.writeValueAsString(ReportScheduleRequestDTO.builder().build());

    mockMvc.perform(post("/reports/schedule")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteSchedule_returns204() throws Exception {
    UUID id = UUID.fromString("7b6a5c4d-3e2f-1a0b-9c8d-7e6f5a4b3c2d");

    mockMvc.perform(delete("/reports/schedule/{id}", id))
        .andExpect(status().isNoContent());

    verify(service).delete(id);
  }
}
