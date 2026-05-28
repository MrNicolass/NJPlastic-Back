package com.njplastic.njplastic_api.config.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;
import com.njplastic.njplastic_api.config.exceptions.BaseApiConflictException;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/auth/login");
    when(request.getMethod()).thenReturn("POST");
  }

  @Test
  void handleBaseApiException_usesExceptionStatusAndMessage() {
    BaseApiConflictException ex = new BaseApiConflictException("already exists");

    ResponseEntity<ErrorResponseDTO> response =
        handler.handleBaseApiBadRequestExceptions(request, ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("already exists");
    assertThat(response.getBody().getClazzError()).isEqualTo("BaseApiConflictException");
    assertThat(response.getBody().getTimestamp()).isNotNull();
  }

  @Test
  void handleMissingServletRequestParameter_returns400() {
    MissingServletRequestParameterException ex =
        new MissingServletRequestParameterException("login", "String");

    ResponseEntity<ErrorResponseDTO> response =
        handler.handleMissingServletRequestParameterException(request, ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getClazzError())
        .isEqualTo("MissingServletRequestParameterException");
  }

  @Test
  void handleMethodArgumentNotValid_returns400WithFieldMessage() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("loginRequest", "password", "size must be at least 12");
    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<ErrorResponseDTO> response =
        handler.handleMethodArgumentNotValidException(request, ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage()).isEqualTo("password: size must be at least 12");
  }

  @Test
  void handleHttpMessageNotReadable_returns422() {
    HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
    when(ex.getMessage()).thenReturn("malformed JSON");

    ResponseEntity<ErrorResponseDTO> response =
        handler.handleHttpMessageNotReadableException(request, ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody().getMessage()).isEqualTo("malformed JSON");
  }
}
