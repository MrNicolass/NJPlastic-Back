package com.njplastic.njplastic_api.config.exceptions;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base API exception that maps to HTTP 422 Unprocessable Entity. Use it (or
 * subclass it) when the request is syntactically valid but cannot be processed
 * because of a domain invariant - for example, a state transition that is not
 * allowed from the current machine status.
 */
public class BaseApiUnprocessableEntityException extends BaseApiException {

  public BaseApiUnprocessableEntityException() {
    super(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  public BaseApiUnprocessableEntityException(String message) {
    super(message, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  public BaseApiUnprocessableEntityException(List<String> args) {
    super(args, HttpStatus.UNPROCESSABLE_ENTITY);
  }
}
