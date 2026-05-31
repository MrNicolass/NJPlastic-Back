package com.njplastic.njplastic_api.erp.exceptions;

import com.njplastic.njplastic_api.config.exceptions.BaseApiUnprocessableEntityException;

/**
 * Thrown when an action that requires the ERP datasource is triggered while
 * {@code app.datasource.erp.enabled=false}. The status endpoint stays
 * available so the Manager can confirm the disabled state via
 * {@code GET /erp/sync/status}; any write/sync action raises this exception
 * instead of silently no-oping. Maps to HTTP 422 through
 * {@link BaseApiUnprocessableEntityException}.
 */
public class ErpSyncDisabledException extends BaseApiUnprocessableEntityException {

  public ErpSyncDisabledException(String message) {
    super(message);
  }
}