package com.njplastic.njplastic_api.production.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiInternalServerErrorException;

/**
 * Thrown when persisting a machine status transition fails because of an
 * underlying data-access error (connection loss, constraint violation,
 * optimistic lock conflict). Wraps the original {@code DataAccessException} as
 * the cause so it stays in the logs while the API returns a standardized 500
 * body through {@link BaseApiInternalServerErrorException}.
 */
public class MachineStatusPersistenceException extends BaseApiInternalServerErrorException {

  public MachineStatusPersistenceException(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}
