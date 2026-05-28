package com.njplastic.njplastic_api.auth.security;

import java.util.UUID;

import com.njplastic.njplastic_api.auth.enums.UserRole;

/**
 * Principal stored in SecurityContextHolder for every authenticated request.
 * Carries the user attributes needed for scope filtering (sector, shift) per
 * RN02..RN04 without forcing a database round-trip on each call.
 */
public record AuthenticatedUser(
    UUID id,
    String login,
    UserRole role,
    String sector,
    String shift) {
}