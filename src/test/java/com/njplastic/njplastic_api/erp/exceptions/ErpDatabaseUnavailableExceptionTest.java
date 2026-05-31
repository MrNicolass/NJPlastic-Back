package com.njplastic.njplastic_api.erp.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import com.njplastic.njplastic_api.config.exceptions.BaseApiInternalServerErrorException;

class ErpDatabaseUnavailableExceptionTest {

  @Test
  void carriesMessageStatusAndCause() {
    DataAccessResourceFailureException cause = new DataAccessResourceFailureException("db down");
    ErpDatabaseUnavailableException ex = new ErpDatabaseUnavailableException("ERP unreachable", cause);

    assertThat(ex.getMessage()).isEqualTo("ERP unreachable");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(ex.getCause()).isSameAs(cause);
    assertThat(ex).isInstanceOf(BaseApiInternalServerErrorException.class);
  }
}
