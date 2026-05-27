package com.njplastic.njplastic_api.config;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/versioning")
@Tag(name = "Versioning", description = "API metadata: build version and identification")
public class VersioningController {

  @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
  @Operation(summary = "Returns the running API version", description = "Plain-text semantic version of the running build. Used by the frontend version banner and by smoke checks on deployments without Spring Boot Actuator.")
  @ApiResponse(responseCode = "200", description = "Semantic version of the API", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", example = "1.0.0")))
  public String getVersion() {
    return "1.0.0";
  }
}