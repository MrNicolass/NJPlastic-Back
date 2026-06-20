package com.njplastic.njplastic_api.production.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiUnprocessableEntityException;

/**
 * Thrown when {@code PUT /machines/{id}/stops/{stopId}/message} targets a
 * record whose state is not AUTO_STOPPED. restricts editable messages
 * to auto-stop entries, so any other state is rejected. Maps to
 * HTTP 422 through {@link BaseApiUnprocessableEntityException}.
 */
public class StopMessageNotEditableException extends BaseApiUnprocessableEntityException {

  public StopMessageNotEditableException(String message) {
    super(message);
  }
}