package com.njplastic.njplastic_api.config.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BaseApiExceptionTest {

  @Test
  void defaultStatus_isBadRequest() {
    BaseApiException ex = new BaseApiException("boom");
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getMessage()).isEqualTo("boom");
  }

  @Test
  void messageAndStatusConstructor_setsBoth() {
    BaseApiException ex = new BaseApiException("nope", HttpStatus.CONFLICT);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(ex.getMessage()).isEqualTo("nope");
  }

  @Test
  void existsArgs_isFalseWhenNoArgs() {
    assertThat(new BaseApiException("x").existsArgs()).isFalse();
  }

  @Test
  void existsArgs_isTrueAndGetArgsReturnsValues() {
    BaseApiException ex = new BaseApiException(List.of("a", "b"));
    assertThat(ex.existsArgs()).isTrue();
    assertThat(ex.getArgs()).containsExactly("a", "b");
  }

  @Test
  void statusOnlyConstructor_keepsStatus() {
    BaseApiException ex = new BaseApiException(HttpStatus.NOT_FOUND);
    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
