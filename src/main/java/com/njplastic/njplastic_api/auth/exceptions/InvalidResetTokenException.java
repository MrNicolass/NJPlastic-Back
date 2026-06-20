package com.njplastic.njplastic_api.auth.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiBadRequestException;

/**
 * Thrown by the password-reset confirm flow when the supplied token does not
 * match any pending record. Returned as HTTP 400 - the message is generic to
 * prevent enumeration of valid tokens (OWASP A07).
 */
public class InvalidResetTokenException extends BaseApiBadRequestException {

  private static final String MESSAGE = "Invalid or unknown password reset token";

  public InvalidResetTokenException() {
    super(MESSAGE);
  }
}
