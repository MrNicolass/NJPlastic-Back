package com.njplastic.njplastic_api.erp.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.njplastic.njplastic_api.erp.dtos.ErpFieldMappingUpdateRequestDTO;
import com.njplastic.njplastic_api.erp.dtos.ErpFieldMappingUpdateRequestDTO.MappingEntry;
import com.njplastic.njplastic_api.erp.entities.ErpFieldMapping;
import com.njplastic.njplastic_api.erp.services.ErpFieldMappingService;

@WebMvcTest(ErpFieldMappingController.class)
@AutoConfigureMockMvc(addFilters = false)
class ErpFieldMappingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @MockitoBean
  private ErpFieldMappingService service;

  private static final UUID PRINCIPAL_ID = UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c");

  @BeforeEach
  void setUp() {
    AuthenticatedUser principal = new AuthenticatedUser(PRINCIPAL_ID, "manager", UserRole.MANAGER, null, null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getMapping_returnsListProjectedFromEntities() throws Exception {
    ErpFieldMapping row = ErpFieldMapping.builder()
        .id(UUID.randomUUID())
        .entityType("PRODUCTION_ORDER")
        .njField("erpOrderId")
        .erpField("id_op")
        .dataType("STRING")
        .required(true)
        .updatedAt(OffsetDateTime.parse("2026-06-01T08:30:00Z"))
        .build();
    when(service.getMapping("PRODUCTION_ORDER")).thenReturn(List.of(row));

    mockMvc.perform(get("/erp/field-mapping").param("entityType", "PRODUCTION_ORDER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].entityType").value("PRODUCTION_ORDER"))
        .andExpect(jsonPath("$[0].njField").value("erpOrderId"))
        .andExpect(jsonPath("$[0].erpField").value("id_op"))
        .andExpect(jsonPath("$[0].dataType").value("STRING"))
        .andExpect(jsonPath("$[0].required").value(true));
  }

  @Test
  void replaceMapping_persistsListAndReturnsProjection() throws Exception {
    ErpFieldMappingUpdateRequestDTO request = ErpFieldMappingUpdateRequestDTO.builder()
        .entityType("PRODUCTION_ORDER")
        .mappings(List.of(
            MappingEntry.builder().njField("erpOrderId").erpField("id_op").dataType("STRING").required(true).build()))
        .build();
    ErpFieldMapping saved = ErpFieldMapping.builder()
        .id(UUID.randomUUID()).entityType("PRODUCTION_ORDER").njField("erpOrderId")
        .erpField("id_op").dataType("STRING").required(true)
        .updatedBy(PRINCIPAL_ID).updatedAt(OffsetDateTime.now()).build();
    when(service.replaceMapping(any(ErpFieldMappingUpdateRequestDTO.class), nullable(UUID.class)))
        .thenReturn(List.of(saved));

    mockMvc.perform(put("/erp/field-mapping")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].njField").value("erpOrderId"));

    verify(service).replaceMapping(any(ErpFieldMappingUpdateRequestDTO.class), nullable(UUID.class));
  }

  @Test
  void replaceMapping_returns400OnBlankEntityType() throws Exception {
    ErpFieldMappingUpdateRequestDTO request = ErpFieldMappingUpdateRequestDTO.builder()
        .entityType("")
        .mappings(List.of())
        .build();

    mockMvc.perform(put("/erp/field-mapping")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
