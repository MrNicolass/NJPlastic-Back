package com.njplastic.njplastic_api.erp.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiUnprocessableEntityException;

class ErpSyncDisabledExceptionTest {

  @Test
  void carriesMessageAndUnprocessableEntityStatus() {
    ErpSyncDisabledException ex = new ErpSyncDisabledException("ERP datasource disabled");

    assertThat(ex.getMessage()).isEqualTo("ERP datasource disabled");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(ex).isInstanceOf(BaseApiUnprocessableEntityException.class);
  }
}
