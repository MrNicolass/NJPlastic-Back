package com.njplastic.njplastic_api.production.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiConflictException;

/**
 * Thrown when {@code POST /machines} attempts to register a machine whose
 * short code already exists in the database. The {@code code} column is
 * globally unique (RFC §5.2.1).
 */
public class MachineAlreadyExistsException extends BaseApiConflictException {

  public MachineAlreadyExistsException(String code) {
    super("Machine already exists with code: " + code);
  }
}
