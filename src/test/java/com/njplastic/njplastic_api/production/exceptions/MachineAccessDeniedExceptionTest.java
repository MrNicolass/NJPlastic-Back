package com.njplastic.njplastic_api.production.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiForbiddenException;

class MachineAccessDeniedExceptionTest {

  @Test
  void carriesMessageAndForbiddenStatus() {
    MachineAccessDeniedException ex = new MachineAccessDeniedException("denied");
    assertThat(ex.getMessage()).isEqualTo("denied");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(ex).isInstanceOf(BaseApiForbiddenException.class);
  }
}
