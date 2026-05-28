package com.njplastic.njplastic_api.auth.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class UserRoleTest {

  @Test
  void getDescription_equalsName() {
    for (UserRole role : UserRole.values()) {
      assertThat(role.getDescription()).isEqualTo(role.name());
    }
  }

  @Test
  void findByDescription_matchesIgnoringCase() {
    assertThat(UserRole.findByDescription("manager")).contains(UserRole.MANAGER);
    assertThat(UserRole.findByDescription("LEADER")).contains(UserRole.LEADER);
    assertThat(UserRole.findByDescription("Operator")).contains(UserRole.OPERATOR);
  }

  @Test
  void findByDescription_returnsEmptyForUnknown() {
    assertThat(UserRole.findByDescription("ADMIN")).isEmpty();
  }

  @Test
  void findByDescription_returnsEmptyForNull() {
    assertThat(UserRole.findByDescription(null)).isEqualTo(Optional.empty());
  }
}
