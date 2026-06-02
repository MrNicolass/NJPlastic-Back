package com.njplastic.njplastic_api.auth.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiBadRequestException;

/**
 * Thrown by the password-reset confirm flow when the token exists but is
 * outside its TTL window. Returned as HTTP 400 with a message distinct from
 * {@link InvalidResetTokenException} so the frontend can prompt the user to
 * request a new email.
 */
public class ExpiredResetTokenException extends BaseApiBadRequestException {

  private static final String MESSAGE = "Password reset token has expired";

  public ExpiredResetTokenException() {
    super(MESSAGE);
  }
}
