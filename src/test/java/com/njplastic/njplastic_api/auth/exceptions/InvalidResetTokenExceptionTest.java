package com.njplastic.njplastic_api.auth.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiBadRequestException;

class InvalidResetTokenExceptionTest {

  @Test
  void carriesFixedMessageAndBadRequestStatus() {
    InvalidResetTokenException ex = new InvalidResetTokenException();
    assertThat(ex.getMessage()).isEqualTo("Invalid or unknown password reset token");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex).isInstanceOf(BaseApiBadRequestException.class);
  }
}
