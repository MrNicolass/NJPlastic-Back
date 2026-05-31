package com.njplastic.njplastic_api.production.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiNotFoundException;

class StopNotFoundExceptionTest {

  @Test
  void carriesMessageAndNotFoundStatus() {
    StopNotFoundException ex = new StopNotFoundException("Stop not found: 42");
    assertThat(ex.getMessage()).isEqualTo("Stop not found: 42");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ex).isInstanceOf(BaseApiNotFoundException.class);
  }
}
