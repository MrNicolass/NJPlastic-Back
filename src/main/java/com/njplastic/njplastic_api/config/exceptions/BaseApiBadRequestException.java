package com.njplastic.njplastic_api.config.exceptions;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base API exception that maps to HTTP 400 Bad Request. Use it (or subclass
 * it) whenever the request itself is malformed at the domain level - missing
 * required business data, invalid combinations, or values that pass syntactic
 * validation but fail semantic rules.
 */
public class BaseApiBadRequestException extends BaseApiException {

  public BaseApiBadRequestException() {
    super(HttpStatus.BAD_REQUEST);
  }

  public BaseApiBadRequestException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }

  public BaseApiBadRequestException(List<String> args) {
    super(args, HttpStatus.BAD_REQUEST);
  }
}
