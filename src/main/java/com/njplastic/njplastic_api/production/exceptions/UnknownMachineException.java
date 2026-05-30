package com.njplastic.njplastic_api.production.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiNotFoundException;

/**
 * Thrown when a machine cannot be resolved by its identifier or Arduino code.
 * Maps to HTTP 404 through {@link BaseApiNotFoundException}.
 */
public class UnknownMachineException extends BaseApiNotFoundException {

  public UnknownMachineException(String message) {
    super(message);
  }
}