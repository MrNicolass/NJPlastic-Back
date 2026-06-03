package com.njplastic.njplastic_api.auth.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiConflictException;

class UserAlreadyExistsExceptionTest {

  @Test
  void mentionsFieldAndCarriesConflictStatus() {
    UserAlreadyExistsException ex = new UserAlreadyExistsException("email");
    assertThat(ex.getMessage()).isEqualTo("User already exists with the supplied email");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(ex).isInstanceOf(BaseApiConflictException.class);
  }
}
