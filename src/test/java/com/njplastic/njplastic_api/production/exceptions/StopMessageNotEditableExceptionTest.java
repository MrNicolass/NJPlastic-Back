package com.njplastic.njplastic_api.production.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiUnprocessableEntityException;

class StopMessageNotEditableExceptionTest {

  @Test
  void carriesMessageAndUnprocessableEntityStatus() {
    StopMessageNotEditableException ex = new StopMessageNotEditableException("not AUTO_STOPPED");
    assertThat(ex.getMessage()).isEqualTo("not AUTO_STOPPED");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(ex).isInstanceOf(BaseApiUnprocessableEntityException.class);
  }
}
