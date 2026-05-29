package com.njplastic.njplastic_api.audit.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.njplastic.njplastic_api.audit.entities.AuditLog;
import com.njplastic.njplastic_api.audit.services.AuditService;
import com.njplastic.njplastic_api.audit.services.PayloadSanitizer;
import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuditFilterTest {

  @Mock
  private AuditService auditService;

  @Mock
  private PayloadSanitizer payloadSanitizer;

  private AuditFilter auditFilter;

  @BeforeEach
  void setUp() {
    auditFilter = new AuditFilter(auditService, payloadSanitizer);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldNotFilter_skipsOptionsRequests() {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/auth/login");

    assertThat(auditFilter.shouldNotFilter(request)).isTrue();
  }

  @Test
  void shouldNotFilter_skipsSwaggerAndApiDocs() {
    MockHttpServletRequest swagger = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
    MockHttpServletRequest apiDocs = new MockHttpServletRequest("GET", "/v3/api-docs");

    assertThat(auditFilter.shouldNotFilter(swagger)).isTrue();
    assertThat(auditFilter.shouldNotFilter(apiDocs)).isTrue();
  }

  @Test
  void shouldNotFilter_processesRegularRequests() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");

    assertThat(auditFilter.shouldNotFilter(request)).isFalse();
  }

  @Test
  void doFilterInternal_persistsAuditLogWithRequestMetadata() throws Exception {
    when(payloadSanitizer.sanitize(any(), any())).thenReturn("{\"x\":1}");
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
    request.setQueryString("debug=true");
    request.setRemoteAddr("10.0.0.5");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(201);

    auditFilter.doFilterInternal(request, response, chain);

    AuditLog captured = captureSavedLog();
    assertThat(captured.getHttpMethod()).isEqualTo("POST");
    assertThat(captured.getEndpoint()).isEqualTo("/auth/login?debug=true");
    assertThat(captured.getHttpStatus()).isEqualTo(201);
    assertThat(captured.getSourceIp()).isEqualTo("10.0.0.5");
    assertThat(captured.getUserId()).isNull();
    assertThat(captured.getDurationMs()).isNotNull();
  }

  @Test
  void doFilterInternal_usesForwardedForAndAuthenticatedUser() throws Exception {
    when(payloadSanitizer.sanitize(any(), any())).thenReturn(null);
    UUID userId = UUID.fromString("3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c");
    AuthenticatedUser principal =
        new AuthenticatedUser(userId, "manager", UserRole.MANAGER, "INJECAO", "TURNO_A");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null));
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/production");
    request.addHeader("X-Forwarded-For", "203.0.113.7, 70.41.3.18");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> {
    };

    auditFilter.doFilterInternal(request, response, chain);

    AuditLog captured = captureSavedLog();
    assertThat(captured.getUserId()).isEqualTo(userId);
    assertThat(captured.getSourceIp()).isEqualTo("203.0.113.7");
    assertThat(captured.getEndpoint()).isEqualTo("/production");
  }

  private AuditLog captureSavedLog() {
    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditService).saveAudit(captor.capture());
    return captor.getValue();
  }
}
