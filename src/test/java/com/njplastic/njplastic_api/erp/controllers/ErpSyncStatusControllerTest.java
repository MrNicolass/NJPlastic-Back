package com.njplastic.njplastic_api.erp.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.njplastic.njplastic_api.erp.dtos.ErpSyncStatusResponseDTO;
import com.njplastic.njplastic_api.erp.enums.ErpConnectionStatus;
import com.njplastic.njplastic_api.erp.services.ErpStatusService;

@WebMvcTest(ErpSyncStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
class ErpSyncStatusControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ErpStatusService erpStatusService;

  @Test
  void getStatus_returnsAggregatedKpis() throws Exception {
    ErpSyncStatusResponseDTO dto = ErpSyncStatusResponseDTO.builder()
        .status(ErpConnectionStatus.OPERATIONAL)
        .lastSyncAt(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .nextWindowAt(OffsetDateTime.parse("2026-05-28T14:01:00Z"))
        .successRate24h(new BigDecimal("0.9583"))
        .avgLatencyMs(1840)
        .ordersReadLastRun(12)
        .cyclesWrittenLastRun(240)
        .pausesWrittenLastRun(5)
        .lastErrorMessage(null)
        .recentRuns(List.of())
        .build();
    when(erpStatusService.buildStatus()).thenReturn(dto);

    mockMvc.perform(get("/erp/sync/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OPERATIONAL"))
        .andExpect(jsonPath("$.avgLatencyMs").value(1840))
        .andExpect(jsonPath("$.ordersReadLastRun").value(12))
        .andExpect(jsonPath("$.cyclesWrittenLastRun").value(240))
        .andExpect(jsonPath("$.pausesWrittenLastRun").value(5));
  }

  @Test
  void getStatus_returnsDisabledWhenFlagOff() throws Exception {
    ErpSyncStatusResponseDTO dto = ErpSyncStatusResponseDTO.builder()
        .status(ErpConnectionStatus.DISABLED)
        .recentRuns(List.of())
        .build();
    when(erpStatusService.buildStatus()).thenReturn(dto);

    mockMvc.perform(get("/erp/sync/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISABLED"));
  }
}
