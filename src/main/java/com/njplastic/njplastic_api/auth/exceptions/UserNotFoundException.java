package com.njplastic.njplastic_api.auth.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiNotFoundException;

/**
 * Thrown when an admin lookup or mutation targets a user UUID that does not
 * exist in {@code users}. Distinct from {@link InvalidCredentialsException} -
 * this is an authenticated, authorized admin flow, so the 404 carries the
 * actual reason.
 */
public class UserNotFoundException extends BaseApiNotFoundException {

  private static final String MESSAGE = "User not found";

  public UserNotFoundException() {
    super(MESSAGE);
  }
}
