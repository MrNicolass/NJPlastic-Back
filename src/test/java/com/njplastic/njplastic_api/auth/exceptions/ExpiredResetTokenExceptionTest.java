package com.njplastic.njplastic_api.auth.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiBadRequestException;

class ExpiredResetTokenExceptionTest {

  @Test
  void carriesFixedMessageAndBadRequestStatus() {
    ExpiredResetTokenException ex = new ExpiredResetTokenException();
    assertThat(ex.getMessage()).isEqualTo("Password reset token has expired");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex).isInstanceOf(BaseApiBadRequestException.class);
  }
}
