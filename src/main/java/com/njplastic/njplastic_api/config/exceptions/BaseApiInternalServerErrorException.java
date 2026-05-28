package com.njplastic.njplastic_api.config.exceptions;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base API exception that maps to HTTP 500 Internal Server Error. Use it (or
 * subclass it) only when the failure is genuinely an internal fault that the
 * caller cannot fix by retrying with different input - never as a catch-all
 * for cases that have a more specific HTTP status.
 */
public class BaseApiInternalServerErrorException extends BaseApiException {

  public BaseApiInternalServerErrorException() {
    super(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public BaseApiInternalServerErrorException(String message) {
    super(message, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public BaseApiInternalServerErrorException(List<String> args) {
    super(args, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
