package com.njplastic.njplastic_api.production.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.njplastic.njplastic_api.production.dtos.EventRequestDTO;
import com.njplastic.njplastic_api.production.entities.ProductionEvent;
import com.njplastic.njplastic_api.production.enums.EventType;
import com.njplastic.njplastic_api.production.services.MachineService;
import com.njplastic.njplastic_api.production.services.ProductionEventService;

@WebMvcTest(ProductionEventController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductionEventControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @MockitoBean
  private ProductionEventService eventService;

  @MockitoBean
  private MachineService machineService;

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

  private ProductionEvent sampleEvent(UUID machineId) {
    return ProductionEvent.builder()
        .id(UUID.fromString("8a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d"))
        .machineId(machineId)
        .userId(PRINCIPAL_ID)
        .type(EventType.TRAINING)
        .description("Treinamento de novo operador")
        .startedAt(OffsetDateTime.parse("2026-06-01T10:00:00Z"))
        .endedAt(OffsetDateTime.parse("2026-06-01T11:30:00Z"))
        .createdAt(OffsetDateTime.parse("2026-06-01T10:00:05Z"))
        .build();
  }

  @Test
  void register_returns201WhenValid() throws Exception {
    UUID machineId = UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d");
    ProductionEvent saved = sampleEvent(machineId);
    when(eventService.register(any(), any(), any(), any(), any(), any())).thenReturn(saved);

    String body = objectMapper.writeValueAsString(EventRequestDTO.builder()
        .machineId(machineId)
        .type(EventType.TRAINING)
        .description("Treinamento de novo operador")
        .startedAt(OffsetDateTime.parse("2026-06-01T10:00:00Z"))
        .endedAt(OffsetDateTime.parse("2026-06-01T11:30:00Z"))
        .build());

    mockMvc.perform(post("/events")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("8a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d"))
        .andExpect(jsonPath("$.type").value("TRAINING"));
  }

  @Test
  void register_returns400OnInvalidPayload() throws Exception {
    String body = objectMapper.writeValueAsString(EventRequestDTO.builder().build());

    mockMvc.perform(post("/events")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listForMachine_returnsPage() throws Exception {
    UUID machineId = UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d");
    ProductionEvent event = sampleEvent(machineId);
    when(eventService.findPaged(eq(machineId), isNull(), isNull(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(event)));

    mockMvc.perform(get("/machines/{machineId}/events", machineId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].type").value("TRAINING"));
  }

  @Test
  void listForMachine_forwardsDateWindow() throws Exception {
    UUID machineId = UUID.fromString("9a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d");
    when(eventService.findPaged(eq(machineId), any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/machines/{machineId}/events", machineId)
            .param("from", "2026-06-01T00:00:00Z")
            .param("to", "2026-06-01T23:59:59Z"))
        .andExpect(status().isOk());

    verify(eventService).findPaged(
        eq(machineId),
        any(OffsetDateTime.class),
        any(OffsetDateTime.class),
        any(Pageable.class));
  }
}
