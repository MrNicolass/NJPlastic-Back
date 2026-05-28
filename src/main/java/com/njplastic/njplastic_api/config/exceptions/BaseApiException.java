package com.njplastic.njplastic_api.config.exceptions;

import org.springframework.http.HttpStatus;

import java.util.List;

public class BaseApiException extends RuntimeException {
  private List<String> args;
  private HttpStatus status = HttpStatus.BAD_REQUEST;

  public BaseApiException() {
    super();
  }

  public BaseApiException(String message) {
    super(message);
  }

  public BaseApiException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }

  public BaseApiException(HttpStatus status) {
    super();
    this.status = status;
  }

  public BaseApiException(List<String> args) {
    super();
    this.args = args;
  }

  public BaseApiException(List<String> args, HttpStatus status) {
    super();
    this.args = args;
    this.status = status;
  }

  public boolean existsArgs() {
    return args != null && !args.isEmpty();
  }

  public Object[] getArgs() {
    return args.toArray(new Object[0]);
  }

  public HttpStatus getStatus() {
    return status;
  }
}