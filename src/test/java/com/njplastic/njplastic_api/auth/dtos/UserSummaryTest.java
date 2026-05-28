package com.njplastic.njplastic_api.auth.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;

class UserSummaryTest {

  private User sampleUser() {
    return User.builder()
        .id(UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c"))
        .login("manager")
        .name("Manager Default")
        .email("manager@njplastic.com")
        .passwordHash("$2a$12$secret")
        .role(UserRole.MANAGER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .active(true)
        .build();
  }

  @Test
  void from_copiesNonSensitiveFields() {
    UserSummary summary = UserSummary.from(sampleUser());
    assertThat(summary.getId()).isEqualTo(UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c"));
    assertThat(summary.getLogin()).isEqualTo("manager");
    assertThat(summary.getName()).isEqualTo("Manager Default");
    assertThat(summary.getRole()).isEqualTo(UserRole.MANAGER);
    assertThat(summary.getSector()).isEqualTo("INJECAO");
    assertThat(summary.getShift()).isEqualTo("TURNO_A");
  }

  @Test
  void toString_doesNotLeakEmailOrPassword() {
    String text = UserSummary.from(sampleUser()).toString();
    assertThat(text).doesNotContain("manager@njplastic.com");
    assertThat(text).doesNotContain("secret");
    assertThat(text).contains("login=manager");
  }
}
