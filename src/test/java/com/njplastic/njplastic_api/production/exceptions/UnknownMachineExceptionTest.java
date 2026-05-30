package com.njplastic.njplastic_api.production.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiException;
import com.njplastic.njplastic_api.config.exceptions.BaseApiNotFoundException;

class UnknownMachineExceptionTest {

  @Test
  void carriesMessageAndNotFoundStatus() {
    UnknownMachineException ex = new UnknownMachineException("Maquina nao encontrada");

    assertThat(ex.getMessage()).isEqualTo("Maquina nao encontrada");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ex)
        .isInstanceOf(BaseApiNotFoundException.class)
        .isInstanceOf(BaseApiException.class);
  }
}
