package com.njplastic.njplastic_api.common.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standardized error response structure for all API exception handlers This
 * class provides a consistent format for error responses across different
 * modules including global, webhook, lwm2mserver, firmware, and other future
 * modules.
 */
@Schema(description = "Standard response for API errors")
public class ErrorResponseDTO {

  @Schema(description = "Date and time when the error occurred", example = "2025-06-20T14:30:15.123", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private LocalDateTime timestamp;

  @Schema(description = "Descriptive error message", example = "Resource not found", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String message;

  @Schema(description = "Detailed error description", example = "The requested resource was not found on the server.", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String description;

  @Schema(description = "Name of the exception class that generated the error", example = "ResourceNotFoundException", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String clazzError;

 /**
 * @return The date and time when the error occurred
 */
  public LocalDateTime getTimestamp() {
    return timestamp;
  }

 /**
 * @return The descriptive error message
 */
  public String getMessage() {
    return message;
  }

 /**
 * @return The detailed error description
 */
  public String getDescription() {
    return description;
  }

 /**
 * @return The name of the exception class that generated the error
 */
  public String getClazzError() {
    return clazzError;
  }

 /**
 * Private constructor that receives a Builder for creating the response
 *
 * @param builder
 * The builder with the data for creating the error response
 */
  private ErrorResponseDTO(Builder builder) {
    this.timestamp = LocalDateTime.now();
    this.message = builder.message;
    this.description = builder.description;
    this.clazzError = builder.clazzError;
  }

 /**
 * @return A new builder for creating instances of ErrorResponseDTO
 */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ErrorResponseDTO{timestamp=").append(timestamp)
        .append(", message=").append(message)
        .append(", description=").append(description)
        .append(", clazzError=").append(clazzError)
        .append('}');
    return sb.toString();
  }

 /**
 * Builder to facilitate the creation of ErrorResponseDTO instances Implements
 * the Builder pattern for fluent construction of ErrorResponseDTO objects
 */
  @Schema(description = "Builder for creating error responses")
  public static class Builder {
    @Schema(description = "Descriptive error message")
    private String message;

    @Schema(description = "Detailed error description")
    private String description;

    @Schema(description = "Name of the exception class that generated the error")
    private String clazzError;

    @Schema(description = "Date and time when the error occurred")
    private LocalDateTime timestamp;

 /**
 * Sets the error message
 *
 * @param message
 * The error message
 * @return The builder itself for method chaining
 */
    public Builder message(String message) {
      this.message = message;
      return this;
    }

 /**
 * Sets the error class name
 *
 * @param clazzError
 * The name of the error class
 * @return The builder itself for method chaining
 */
    public Builder clazzError(String clazzError) {
      this.clazzError = clazzError;
      return this;
    }

 /**
 * Sets the error description
 *
 * @param description
 * The error description
 * @return The builder itself for method chaining
 */
    public Builder description(String description) {
      this.description = description;
      return this;
    }

 /**
 * Builds an instance of ErrorResponseDTO with the configured data
 *
 * @return A new instance of ErrorResponseDTO
 */
    public ErrorResponseDTO build() {
      return new ErrorResponseDTO(this);
    }
  }
}