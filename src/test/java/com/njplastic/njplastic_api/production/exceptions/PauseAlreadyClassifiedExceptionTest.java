package com.njplastic.njplastic_api.production.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiConflictException;

class PauseAlreadyClassifiedExceptionTest {

  @Test
  void carriesMessageAndConflictStatus() {
    PauseAlreadyClassifiedException ex = new PauseAlreadyClassifiedException("nothing pending");
    assertThat(ex.getMessage()).isEqualTo("nothing pending");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(ex).isInstanceOf(BaseApiConflictException.class);
  }
}
