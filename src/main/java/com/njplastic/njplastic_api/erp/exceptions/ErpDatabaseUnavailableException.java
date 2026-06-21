package com.njplastic.njplastic_api.erp.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiInternalServerErrorException;

/**
 * Thrown by {@code ErpDatabaseRepository} when a JDBC call against the ERP
 * fails (connection refused, timeout, expired credential, vendor SQL error).
 * Wraps the underlying {@code DataAccessException}/{@code SQLException} as the
 * cause so it stays in the logs while the API returns the standard 500 body
 * via {@link BaseApiInternalServerErrorException}. The ERP sync flow runs
 * outside the GlobalExceptionHandler scope (MQTT/@Scheduled context), so the
 * scheduler also catches this exception to register the failure in
 * {@code erp_sync_run} without crashing the JVM.
 */
public class ErpDatabaseUnavailableException extends BaseApiInternalServerErrorException {

  public ErpDatabaseUnavailableException(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}