package com.njplastic.njplastic_api.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

  @Mock
  private ObjectMapper objectMapper;

  private SecurityConfig securityConfig;

  @BeforeEach
  void setUp() {
    securityConfig = new SecurityConfig(objectMapper);
  }

  @Test
  void passwordEncoderHashesAndMatches() {
    PasswordEncoder encoder = securityConfig.passwordEncoder();

    String encoded = encoder.encode("manager-dev-123");

    assertThat(encoded).isNotEqualTo("manager-dev-123");
    assertThat(encoder.matches("manager-dev-123", encoded)).isTrue();
    assertThat(encoder.matches("wrong-password", encoded)).isFalse();
  }

  @Test
  void corsConfigurationAppliesConfiguredOrigins() {
    CorsProperties properties = new CorsProperties(List.of("https://app.njplastic.com"));

    UrlBasedCorsConfigurationSource source =
        (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource(properties);
    CorsConfiguration cfg = source.getCorsConfigurations().get("/**");

    assertThat(cfg).isNotNull();
    assertThat(cfg.getAllowedOrigins()).containsExactly("https://app.njplastic.com");
    assertThat(cfg.getAllowedMethods())
        .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    assertThat(cfg.getAllowedHeaders()).containsExactly("Authorization", "Content-Type", "Accept");
    assertThat(cfg.getExposedHeaders()).containsExactly("Authorization");
    assertThat(cfg.getAllowCredentials()).isTrue();
    assertThat(cfg.getMaxAge()).isEqualTo(3600L);
  }

  @Test
  void corsConfigurationLeavesOriginsUnsetWhenEmpty() {
    CorsProperties properties = new CorsProperties(List.of());

    UrlBasedCorsConfigurationSource source =
        (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource(properties);
    CorsConfiguration cfg = source.getCorsConfigurations().get("/**");

    assertThat(cfg.getAllowedOrigins()).isNull();
  }

  @Test
  void corsConfigurationLeavesOriginsUnsetWhenNull() {
    CorsProperties properties = new CorsProperties(null);

    UrlBasedCorsConfigurationSource source =
        (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource(properties);
    CorsConfiguration cfg = source.getCorsConfigurations().get("/**");

    assertThat(cfg.getAllowedOrigins()).isNull();
  }

  @Test
  void writeUnauthorizedReturns401WithFixedMessage() throws Exception {
    HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
    PrintWriter writer = org.mockito.Mockito.mock(PrintWriter.class);
    when(response.getWriter()).thenReturn(writer);
    AuthenticationException ex = new BadCredentialsException("bad");

    invokePrivate("writeUnauthorized", request, response, ex, AuthenticationException.class);

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");
    ErrorResponseDTO body = captureWrittenBody();
    assertThat(body.getMessage()).isEqualTo("Credenciais inválidas");
    assertThat(body.getClazzError()).isEqualTo("BadCredentialsException");
  }

  @Test
  void writeForbiddenReturns403WithFixedMessage() throws Exception {
    HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
    PrintWriter writer = org.mockito.Mockito.mock(PrintWriter.class);
    when(response.getWriter()).thenReturn(writer);
    AccessDeniedException ex = new AccessDeniedException("denied");

    invokePrivate("writeForbidden", request, response, ex, AccessDeniedException.class);

    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");
    ErrorResponseDTO body = captureWrittenBody();
    assertThat(body.getMessage()).isEqualTo("Acesso negado");
    assertThat(body.getClazzError()).isEqualTo("AccessDeniedException");
  }

  private void invokePrivate(String name, HttpServletRequest request,
      HttpServletResponse response, Object exception, Class<?> exceptionType) throws Exception {
    Method method = SecurityConfig.class.getDeclaredMethod(
        name, HttpServletRequest.class, HttpServletResponse.class, exceptionType);
    method.setAccessible(true);
    method.invoke(securityConfig, request, response, exception);
  }

  private ErrorResponseDTO captureWrittenBody() {
    ArgumentCaptor<ErrorResponseDTO> captor = ArgumentCaptor.forClass(ErrorResponseDTO.class);
    verify(objectMapper).writeValue(any(Writer.class), captor.capture());
    return captor.getValue();
  }
}
