package com.njplastic.njplastic_api.production.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiException;
import com.njplastic.njplastic_api.config.exceptions.BaseApiInternalServerErrorException;

class MachineStatusPersistenceExceptionTest {

  @Test
  void carriesMessageAndInternalServerErrorStatus() {
    RuntimeException cause = new RuntimeException("db down");
    MachineStatusPersistenceException ex = new MachineStatusPersistenceException("Failed to persist", cause);

    assertThat(ex.getMessage()).isEqualTo("Failed to persist");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(ex.getCause()).isSameAs(cause);
    assertThat(ex)
        .isInstanceOf(BaseApiInternalServerErrorException.class)
        .isInstanceOf(BaseApiException.class);
  }
}
