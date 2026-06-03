package com.njplastic.njplastic_api.auth.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;

class UserResponseDTOTest {

  @Test
  void from_copiesEveryFieldExceptPasswordHash() {
    UUID id = UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c");
    User user = User.builder()
        .id(id)
        .login("manager")
        .name("Manager Default")
        .email("manager@njplastic.com")
        .passwordHash("$2a$12$secret-hash")
        .role(UserRole.MANAGER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .active(true)
        .createdAt(OffsetDateTime.parse("2026-05-28T08:30:00Z"))
        .updatedAt(OffsetDateTime.parse("2026-05-28T08:30:00Z"))
        .build();

    UserResponseDTO dto = UserResponseDTO.from(user);

    assertThat(dto.getId()).isEqualTo(id);
    assertThat(dto.getLogin()).isEqualTo("manager");
    assertThat(dto.getName()).isEqualTo("Manager Default");
    assertThat(dto.getEmail()).isEqualTo("manager@njplastic.com");
    assertThat(dto.getRole()).isEqualTo(UserRole.MANAGER);
    assertThat(dto.getSector()).isEqualTo("INJECAO");
    assertThat(dto.getShift()).isEqualTo("TURNO_A");
    assertThat(dto.isActive()).isTrue();
    assertThat(dto.toString()).doesNotContain("$2a$12$secret-hash");
    assertThat(dto.toString()).doesNotContain("passwordHash");
  }
}
