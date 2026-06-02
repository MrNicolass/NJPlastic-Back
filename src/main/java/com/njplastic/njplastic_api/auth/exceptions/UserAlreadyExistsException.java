package com.njplastic.njplastic_api.auth.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiConflictException;

/**
 * Thrown when {@code POST /users} or {@code PUT /users/{id}} would create a
 * conflict on a uniquely-constrained column. The message names the field but
 * never echoes the conflicting value back, keeping the audit trail safe.
 */
public class UserAlreadyExistsException extends BaseApiConflictException {

  public UserAlreadyExistsException(String field) {
    super("User already exists with the supplied " + field);
  }
}
