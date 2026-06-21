package com.njplastic.njplastic_api.config.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;
import com.njplastic.njplastic_api.config.exceptions.BaseApiException;
import com.njplastic.njplastic_api.utils.LoggerUtils;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

  private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

 /**
 * Handles all exceptions that extend BaseApiException.
 * This method captures the exception, logs it, and returns a standardized error
 * response.
 *
 * @param request the HttpServletRequest object
 * @param ex the BaseApiException that was thrown
 * @return a ResponseEntity containing the ErrorResponseDTO with appropriate
 * HTTP status
 */
  @ExceptionHandler(BaseApiException.class)
  public ResponseEntity<ErrorResponseDTO> handleBaseApiBadRequestExceptions(
      HttpServletRequest request, BaseApiException ex) {

    String className = ex.getClass().getSimpleName();

    ErrorResponseDTO.Builder errorResponseDTO = ErrorResponseDTO.builder()
        .message(ex.getMessage())
        .clazzError(className);

    LoggerUtils.dispatchLogException(logger, request, ex.getClass().getSimpleName(), ex.getMessage());

    return new ResponseEntity<>(errorResponseDTO.build(), ex.getStatus());
  }

 /**
 * Handles MissingServletRequestParameterException, which occurs when a required
 * request parameter is missing.
 * Logs the exception and returns a standardized error response with HTTP status
 * 400 Bad Request.
 *
 * @param request the HttpServletRequest object
 * @param ex the MissingServletRequestParameterException that was thrown
 * @return a ResponseEntity containing the ErrorResponseDTO with HTTP status 400
 * Bad Request
 */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponseDTO> handleMissingServletRequestParameterException(
      HttpServletRequest request, MissingServletRequestParameterException ex) {

    ErrorResponseDTO.Builder ErrorResponseDTOBuilder = ErrorResponseDTO.builder()
        .message(ex.getMessage())
        .clazzError(ex.getClass().getSimpleName());

    LoggerUtils.dispatchLogException(logger, request, ex.getClass().getSimpleName(), ex.getMessage());

    return new ResponseEntity<>(ErrorResponseDTOBuilder.build(), HttpStatus.BAD_REQUEST);
  }

 /**
 * Handles MethodArgumentNotValidException, which occurs when a method argument
 * fails validation.
 * Logs the exception and returns a standardized error response with HTTP status
 * 400 Bad Request.
 *
 * @param request the HttpServletRequest object
 * @param ex the MethodArgumentNotValidException that was thrown
 * @return a ResponseEntity containing the ErrorResponseDTO with HTTP status 400
 * Bad Request
 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(
      HttpServletRequest request, MethodArgumentNotValidException ex) {

    String errorMessage = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .orElse(ex.getMessage());

    ErrorResponseDTO.Builder ErrorResponseDTOBuilder = ErrorResponseDTO.builder()
        .message(errorMessage)
        .clazzError(ex.getClass().getSimpleName());

    LoggerUtils.dispatchLogException(logger, request, ex.getClass().getSimpleName(), errorMessage);

    return new ResponseEntity<>(ErrorResponseDTOBuilder.build(), HttpStatus.BAD_REQUEST);
  }

 /**
 * Handles HttpMessageNotReadableException, which occurs when the request body
 * cannot be read.
 * Logs the exception and returns a standardized error response with HTTP status
 * 422 Unprocessable Entity.
 *
 * @param request the HttpServletRequest object
 * @param ex the HttpMessageNotReadableException that was thrown
 * @return a ResponseEntity containing the ErrorResponseDTO with HTTP status 422
 * Unprocessable Entity
 */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadableException(
      HttpServletRequest request, HttpMessageNotReadableException ex) {

    ErrorResponseDTO.Builder ErrorResponseDTOBuilder = ErrorResponseDTO.builder()
        .message(ex.getMessage())
        .clazzError(ex.getClass().getSimpleName());

    LoggerUtils.dispatchLogException(logger, request, ex.getClass().getSimpleName(), ex.getMessage());

    return new ResponseEntity<>(ErrorResponseDTOBuilder.build(), HttpStatus.UNPROCESSABLE_ENTITY);
  }

}