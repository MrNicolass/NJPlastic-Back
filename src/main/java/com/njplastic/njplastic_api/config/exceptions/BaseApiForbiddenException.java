package com.njplastic.njplastic_api.config.exceptions;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base API exception that maps to HTTP 403 Forbidden. Use it (or subclass it)
 * when the caller is authenticated but lacks permission for the requested
 * operation - typical for / violations enforced in the service
 * layer beyond what {@code @PreAuthorize} can express.
 */
public class BaseApiForbiddenException extends BaseApiException {

  public BaseApiForbiddenException() {
    super(HttpStatus.FORBIDDEN);
  }

  public BaseApiForbiddenException(String message) {
    super(message, HttpStatus.FORBIDDEN);
  }

  public BaseApiForbiddenException(List<String> args) {
    super(args, HttpStatus.FORBIDDEN);
  }
}
