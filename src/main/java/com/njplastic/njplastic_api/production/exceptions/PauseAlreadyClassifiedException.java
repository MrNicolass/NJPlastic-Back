package com.njplastic.njplastic_api.production.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiConflictException;

/**
 * Thrown when {@code POST /machines/{id}/pauses} cannot find an open
 * isolated pause without a reason to classify (RF09). Either the machine
 * has no PAUSED record or every PAUSED record already has a reason. Maps
 * to HTTP 409 through {@link BaseApiConflictException}.
 */
public class PauseAlreadyClassifiedException extends BaseApiConflictException {

  public PauseAlreadyClassifiedException(String message) {
    super(message);
  }
}