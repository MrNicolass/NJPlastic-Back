package com.njplastic.njplastic_api.erp.controllers;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.erp.dtos.ProductionOrderSummaryDTO;
import com.njplastic.njplastic_api.erp.entities.ProductionOrderCache;
import com.njplastic.njplastic_api.erp.services.ProductionOrderService;

@WebMvcTest(ProductionOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductionOrderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ProductionOrderService service;

  private static final UUID PRINCIPAL_ID = UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c");

  @BeforeEach
  void setUp() {
    AuthenticatedUser principal = new AuthenticatedUser(
        PRINCIPAL_ID, "leader", UserRole.LEADER, "INJECAO", "TURNO_A");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_LEADER"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void listOrders_returnsPageOfProjections() throws Exception {
    ProductionOrderCache row = ProductionOrderCache.builder()
        .id(UUID.randomUUID())
        .erpOrderId("OP-2026-0001")
        .productCode("PVC-100ML-BR")
        .targetQuantity(5000)
        .status("OPEN")
        .lastSyncAt(OffsetDateTime.parse("2026-05-28T14:00:00Z"))
        .build();
    when(service.findPaged(any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(row)));

    mockMvc.perform(get("/production-orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].erpOrderId").value("OP-2026-0001"))
        .andExpect(jsonPath("$.content[0].productCode").value("PVC-100ML-BR"))
        .andExpect(jsonPath("$.content[0].targetQuantity").value(5000))
        .andExpect(jsonPath("$.content[0].status").value("OPEN"));
  }

  @Test
  void summary_returnsKpiCounters() throws Exception {
    when(service.summarize(any())).thenReturn(
        ProductionOrderSummaryDTO.builder()
            .inProd(8).queued(12).overdue(3).completed(27).build());

    mockMvc.perform(get("/production-orders/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inProd").value(8))
        .andExpect(jsonPath("$.queued").value(12))
        .andExpect(jsonPath("$.overdue").value(3))
        .andExpect(jsonPath("$.completed").value(27));
  }
}
