package com.njplastic.njplastic_api.auth.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiException;

class InvalidCredentialsExceptionTest {

  @Test
  void carriesFixedMessageAndUnauthorizedStatus() {
    InvalidCredentialsException ex = new InvalidCredentialsException();
    assertThat(ex.getMessage()).isEqualTo("Credenciais inválidas");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(ex).isInstanceOf(BaseApiException.class);
  }
}
