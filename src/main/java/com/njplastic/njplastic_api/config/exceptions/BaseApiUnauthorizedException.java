package com.njplastic.njplastic_api.config.exceptions;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base API exception that maps to HTTP 401 Unauthorized. Use it (or subclass
 * it) when the caller failed to prove its identity - missing or invalid
 * credentials, expired JWT, or any case where authentication is required but
 * absent. For "authenticated but not allowed", use
 * {@link BaseApiForbiddenException} instead.
 */
public class BaseApiUnauthorizedException extends BaseApiException {

  public BaseApiUnauthorizedException() {
    super(HttpStatus.UNAUTHORIZED);
  }

  public BaseApiUnauthorizedException(String message) {
    super(message, HttpStatus.UNAUTHORIZED);
  }

  public BaseApiUnauthorizedException(List<String> args) {
    super(args, HttpStatus.UNAUTHORIZED);
  }
}
