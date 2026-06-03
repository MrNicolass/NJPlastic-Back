package com.njplastic.njplastic_api.auth.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.njplastic.njplastic_api.auth.dtos.UserRequestDTO;
import com.njplastic.njplastic_api.auth.dtos.UserUpdateRequestDTO;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.services.UserService;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private PasswordEncoder passwordEncoder;

  private User sampleUser(UUID id) {
    return User.builder()
        .id(id)
        .login("manager")
        .name("Manager Default")
        .email("manager@njplastic.com")
        .passwordHash("$2a$12$hash")
        .role(UserRole.MANAGER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .active(true)
        .createdAt(OffsetDateTime.parse("2026-05-28T08:30:00Z"))
        .updatedAt(OffsetDateTime.parse("2026-05-28T08:30:00Z"))
        .build();
  }

  @Test
  void listUsers_returnsPage() throws Exception {
    User user = sampleUser(UUID.randomUUID());
    when(userService.findPaged(any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(user)));

    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].login").value("manager"))
        .andExpect(jsonPath("$.content[0].role").value("MANAGER"));
  }

  @Test
  void getUser_returns200WhenFound() throws Exception {
    UUID id = UUID.randomUUID();
    when(userService.findById(id)).thenReturn(Optional.of(sampleUser(id)));

    mockMvc.perform(get("/users/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.login").value("manager"));
  }

  @Test
  void getUser_returns404WhenMissing() throws Exception {
    UUID id = UUID.randomUUID();
    when(userService.findById(id)).thenReturn(Optional.empty());

    mockMvc.perform(get("/users/{id}", id))
        .andExpect(status().isNotFound());
  }

  @Test
  void createUser_returns201WhenAllowed() throws Exception {
    when(userService.existsByLogin("manager")).thenReturn(false);
    when(userService.existsByEmail("manager@njplastic.com")).thenReturn(false);
    when(passwordEncoder.encode("manager-dev-123")).thenReturn("hashed");
    User saved = sampleUser(UUID.randomUUID());
    when(userService.create(any(User.class))).thenReturn(saved);

    String body = objectMapper.writeValueAsString(UserRequestDTO.builder()
        .login("manager")
        .name("Manager Default")
        .email("manager@njplastic.com")
        .password("manager-dev-123")
        .role(UserRole.MANAGER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .build());

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.login").value("manager"));
  }

  @Test
  void createUser_returns409WhenLoginExists() throws Exception {
    when(userService.existsByLogin("manager")).thenReturn(true);

    String body = objectMapper.writeValueAsString(UserRequestDTO.builder()
        .login("manager")
        .name("Manager")
        .email("e@x.com")
        .password("manager-dev-123")
        .role(UserRole.MANAGER)
        .build());

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void createUser_returns409WhenEmailExists() throws Exception {
    when(userService.existsByLogin("manager")).thenReturn(false);
    when(userService.existsByEmail("e@x.com")).thenReturn(true);

    String body = objectMapper.writeValueAsString(UserRequestDTO.builder()
        .login("manager")
        .name("Manager")
        .email("e@x.com")
        .password("manager-dev-123")
        .role(UserRole.MANAGER)
        .build());

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void createUser_returns400OnInvalidPayload() throws Exception {
    String body = objectMapper.writeValueAsString(UserRequestDTO.builder()
        .login("")
        .name("")
        .email("not-an-email")
        .password("short")
        .build());

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateUser_returns200WhenFound() throws Exception {
    UUID id = UUID.randomUUID();
    User current = sampleUser(id);
    when(userService.findById(id)).thenReturn(Optional.of(current));
    when(userService.update(eq(id), any(User.class))).thenReturn(current);

    String body = objectMapper.writeValueAsString(UserUpdateRequestDTO.builder()
        .name("Manager Default")
        .email("manager@njplastic.com")
        .role(UserRole.MANAGER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .active(true)
        .build());

    mockMvc.perform(put("/users/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.login").value("manager"));
  }

  @Test
  void updateUser_returns404WhenMissing() throws Exception {
    UUID id = UUID.randomUUID();
    when(userService.findById(id)).thenReturn(Optional.empty());

    String body = objectMapper.writeValueAsString(UserUpdateRequestDTO.builder()
        .name("X")
        .email("manager@njplastic.com")
        .role(UserRole.MANAGER)
        .active(true)
        .build());

    mockMvc.perform(put("/users/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateUser_returns409WhenChangingToTakenEmail() throws Exception {
    UUID id = UUID.randomUUID();
    User current = sampleUser(id);
    when(userService.findById(id)).thenReturn(Optional.of(current));
    when(userService.existsByEmail("taken@njplastic.com")).thenReturn(true);

    String body = objectMapper.writeValueAsString(UserUpdateRequestDTO.builder()
        .name("Manager")
        .email("taken@njplastic.com")
        .role(UserRole.MANAGER)
        .active(true)
        .build());

    mockMvc.perform(put("/users/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void deleteUser_returns204() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/users/{id}", id))
        .andExpect(status().isNoContent());

    verify(userService).softDelete(id);
  }
}
