package com.njplastic.njplastic_api.production.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiNotFoundException;

/**
 * Thrown when {@code PUT /machines/{id}/stops/{stopId}/message} cannot
 * resolve the stop record by its identifier or it does not belong to the
 * machine in the path. Maps to HTTP 404 through
 * {@link BaseApiNotFoundException}.
 */
public class StopNotFoundException extends BaseApiNotFoundException {

  public StopNotFoundException(String message) {
    super(message);
  }
}