package com.njplastic.njplastic_api.auth.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Authorization profile. Mapped to the
 * PostgreSQL ENUM type {@code user_role} declared in V1__init.sql.
 *
 * <p>
 * Follows the project-wide enum convention: each constant carries a
 * {@code description} equal to its {@link #name}, exposed via
 * {@link #getDescription}, with {@link #findByDescription(String)} as the
 * canonical lookup from a string payload.
 */
public enum UserRole {

  OPERATOR("OPERATOR"),
  LEADER("LEADER"),
  MANAGER("MANAGER");

  private final String description;

  UserRole(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

 /**
 * Find a {@link UserRole} by its description, case-insensitive.
 *
 * @param description the description to match against
 * @return the matching role, or empty if none matches
 */
  public static Optional<UserRole> findByDescription(String description) {
    if (description == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(role -> role.description.equalsIgnoreCase(description))
        .findFirst();
  }
}