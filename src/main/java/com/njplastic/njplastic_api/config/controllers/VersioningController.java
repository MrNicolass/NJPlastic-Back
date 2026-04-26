package com.njplastic.njplastic_api.config.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1/versioning")
public class VersioningController {
  
  @GetMapping()
  public String getVersion() {
      return "1.0.0";
  }
  
}