package com.njplastic.njplastic_api.config.exceptions;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base API exception that maps to HTTP 409 Conflict. Use it (or subclass it)
 * when the request collides with the current state of the resource - typical
 * cases are unique-constraint violations and optimistic-lock failures.
 */
public class BaseApiConflictException extends BaseApiException {

  public BaseApiConflictException() {
    super(HttpStatus.CONFLICT);
  }

  public BaseApiConflictException(String message) {
    super(message, HttpStatus.CONFLICT);
  }

  public BaseApiConflictException(List<String> args) {
    super(args, HttpStatus.CONFLICT);
  }
}
