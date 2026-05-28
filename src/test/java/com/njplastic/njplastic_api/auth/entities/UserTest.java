package com.njplastic.njplastic_api.auth.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.auth.enums.UserRole;

class UserTest {

  private User sampleUser() {
    return User.builder()
        .id(UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c"))
        .login("manager")
        .name("Manager Default")
        .email("manager@njplastic.com")
        .passwordHash("$2a$12$averysecretbcrypthashvalue")
        .role(UserRole.MANAGER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .active(true)
        .build();
  }

  @Test
  void toString_masksPasswordHash() {
    String text = sampleUser().toString();
    assertThat(text).contains("passwordHash=***");
    assertThat(text).doesNotContain("averysecretbcrypthashvalue");
  }

  @Test
  void toString_keepsNonSensitiveFields() {
    String text = sampleUser().toString();
    assertThat(text).contains("login=manager");
    assertThat(text).contains("role=MANAGER");
    assertThat(text).contains("sector=INJECAO");
  }

  @Test
  void onCreate_generatesIdAndTimestampsWhenMissing() {
    User user = new User();
    user.onCreate();
    assertThat(user.getId()).isNotNull();
    assertThat(user.getCreatedAt()).isNotNull();
    assertThat(user.getUpdatedAt()).isNotNull();
  }

  @Test
  void onCreate_preservesExistingIdAndCreatedAt() {
    UUID id = UUID.randomUUID();
    OffsetDateTime created = OffsetDateTime.now().minusDays(2);
    User user = User.builder().id(id).createdAt(created).build();
    user.onCreate();
    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getCreatedAt()).isEqualTo(created);
    assertThat(user.getUpdatedAt()).isNotNull();
  }

  @Test
  void onUpdate_refreshesUpdatedAt() {
    User user = new User();
    user.onUpdate();
    assertThat(user.getUpdatedAt()).isNotNull();
  }
}
