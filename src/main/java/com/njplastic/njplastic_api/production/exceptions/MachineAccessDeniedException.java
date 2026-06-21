package com.njplastic.njplastic_api.production.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiForbiddenException;

/**
 * Thrown when an authenticated user is denied access to a machine because
 * its {@code sector} is outside the user's scope (for OPERATOR, 
 * for LEADER). The check happens in the service layer after the principal
 * has already cleared the role-based {@code @PreAuthorize} on the
 * controller. Maps to HTTP 403 through {@link BaseApiForbiddenException}.
 */
public class MachineAccessDeniedException extends BaseApiForbiddenException {

  public MachineAccessDeniedException(String message) {
    super(message);
  }
}