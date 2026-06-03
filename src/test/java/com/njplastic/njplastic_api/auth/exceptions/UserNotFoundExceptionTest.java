package com.njplastic.njplastic_api.auth.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiNotFoundException;

class UserNotFoundExceptionTest {

  @Test
  void carriesFixedMessageAndNotFoundStatus() {
    UserNotFoundException ex = new UserNotFoundException();
    assertThat(ex.getMessage()).isEqualTo("User not found");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ex).isInstanceOf(BaseApiNotFoundException.class);
  }
}
