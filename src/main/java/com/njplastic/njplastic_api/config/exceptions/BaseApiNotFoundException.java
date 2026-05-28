package com.njplastic.njplastic_api.config.exceptions;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base API exception that maps to HTTP 404 Not Found. Use it (or subclass it)
 * when the requested resource does not exist in the application database - or
 * exists but is hidden from the caller by an authorization filter that should
 * not leak its presence.
 */
public class BaseApiNotFoundException extends BaseApiException {

  public BaseApiNotFoundException() {
    super(HttpStatus.NOT_FOUND);
  }

  public BaseApiNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND);
  }

  public BaseApiNotFoundException(List<String> args) {
    super(args, HttpStatus.NOT_FOUND);
  }
}
