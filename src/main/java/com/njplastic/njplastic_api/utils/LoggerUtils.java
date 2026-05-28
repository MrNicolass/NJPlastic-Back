package com.njplastic.njplastic_api.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;

public class LoggerUtils {

  private static final String BASE_LOG_STRING_EXCEPTION = "%s: [%s][%s] - Message [%s]";

  private LoggerUtils() {
  }

  public static void dispatchLogException(Logger logger, HttpServletRequest request, String className, String message) {
    String formattedLog = String.format(BASE_LOG_STRING_EXCEPTION, className, request.getRequestURI(),
        request.getMethod(), message);
    logger.error(formattedLog);
  }
}