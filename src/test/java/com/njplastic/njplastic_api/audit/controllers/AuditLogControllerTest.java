package com.njplastic.njplastic_api.audit.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.njplastic.njplastic_api.audit.entities.AuditLog;
import com.njplastic.njplastic_api.audit.services.AuditService;

@WebMvcTest(AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuditService auditService;

  @Test
  void list_returnsPageOfAuditEntries() throws Exception {
    AuditLog entry = AuditLog.builder()
        .id(UUID.randomUUID())
        .timestamp(OffsetDateTime.parse("2026-05-28T08:30:00Z"))
        .httpMethod("POST")
        .endpoint("/auth/login")
        .httpStatus(200)
        .build();
    when(auditService.findPaged(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entry)));

    mockMvc.perform(get("/audit-logs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].httpMethod").value("POST"))
        .andExpect(jsonPath("$.content[0].endpoint").value("/auth/login"))
        .andExpect(jsonPath("$.content[0].httpStatus").value(200));
  }

  @Test
  void list_forwardsFilterParameters() throws Exception {
    UUID userId = UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c");
    when(auditService.findPaged(eq(userId), eq("/auth"), eq("POST"), eq(200), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/audit-logs")
            .param("userId", userId.toString())
            .param("endpoint", "/auth")
            .param("method", "POST")
            .param("statusCode", "200")
            .param("from", "2026-05-28T00:00:00Z")
            .param("to", "2026-05-29T00:00:00Z"))
        .andExpect(status().isOk());
  }
}
