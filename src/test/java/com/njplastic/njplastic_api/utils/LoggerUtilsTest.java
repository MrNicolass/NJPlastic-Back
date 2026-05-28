package com.njplastic.njplastic_api.utils;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import jakarta.servlet.http.HttpServletRequest;

class LoggerUtilsTest {

  @Test
  void dispatchLogException_logsFormattedMessage() {
    Logger logger = mock(Logger.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/auth/login");
    when(request.getMethod()).thenReturn("POST");

    LoggerUtils.dispatchLogException(logger, request, "InvalidCredentialsException", "Credenciais inválidas");

    verify(logger).error(contains("InvalidCredentialsException: [/auth/login][POST] - Message [Credenciais inválidas]"));
  }
}
