package com.njplastic.njplastic_api.auth.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiUnauthorizedException;

/**
 * Thrown by {@code AuthenticationService} for every login failure - unknown
 * login, inactive user, or wrong password. The message is fixed and identical
 * across all paths to prevent user enumeration (OWASP A07).
 */
public class InvalidCredentialsException extends BaseApiUnauthorizedException {

  private static final String MESSAGE = "Invalid credentials";

  public InvalidCredentialsException() {
    super(MESSAGE);
  }
}
