package com.njplastic.njplastic_api.production.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.production.dtos.ShiftReportResponseDTO;
import com.njplastic.njplastic_api.production.services.ReportService;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ReportService reportService;

  private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-05-28T06:00:00Z");
  private static final OffsetDateTime TO = OffsetDateTime.parse("2026-05-28T14:00:00Z");

  @BeforeEach
  void setUp() {
    AuthenticatedUser principal = new AuthenticatedUser(
        UUID.randomUUID(), "leader", UserRole.LEADER, "INJECAO", "TURNO_A");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_LEADER"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getShiftReport_returnsAggregatedReport() throws Exception {
    ShiftReportResponseDTO report = ShiftReportResponseDTO.builder()
        .periodStart(FROM)
        .periodEnd(TO)
        .sector("INJECAO")
        .shift("TURNO_A")
        .machines(List.of())
        .build();
    when(reportService.buildShiftReport(eq(FROM), eq(TO), eq("INJECAO"), eq("TURNO_A"), any()))
        .thenReturn(report);

    mockMvc.perform(get("/reports/shift")
            .param("from", "2026-05-28T06:00:00Z")
            .param("to", "2026-05-28T14:00:00Z")
            .param("sector", "INJECAO")
            .param("shift", "TURNO_A"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sector").value("INJECAO"))
        .andExpect(jsonPath("$.shift").value("TURNO_A"));
  }

  @Test
  void getShiftReport_acceptsMissingOptionalParams() throws Exception {
    ShiftReportResponseDTO report = ShiftReportResponseDTO.builder()
        .periodStart(FROM)
        .periodEnd(TO)
        .machines(List.of())
        .build();
    when(reportService.buildShiftReport(eq(FROM), eq(TO), eq(null), eq(null), any()))
        .thenReturn(report);

    mockMvc.perform(get("/reports/shift")
            .param("from", "2026-05-28T06:00:00Z")
            .param("to", "2026-05-28T14:00:00Z"))
        .andExpect(status().isOk());
  }
}
