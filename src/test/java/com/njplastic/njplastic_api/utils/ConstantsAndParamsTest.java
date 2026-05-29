package com.njplastic.njplastic_api.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConstantsAndParamsTest {

  @Test
  void headerConstant_hasExpectedValue() {
    assertEquals("X-Forwarded-For", ConstantsAndParams.HTTP_HEADER_FORWARDED_FOR);
  }

  @Test
  void splitterConstants_haveExpectedValues() {
    assertEquals(",", ConstantsAndParams.SPLIT_USER_IP);
    assertEquals("?", ConstantsAndParams.SPLIT_REQUEST_PARAMS);
  }

  @Test
  void encoderConstant_hasExpectedValue() {
    assertEquals("UTF-8", ConstantsAndParams.ENCODER_TEXT);
  }
}
