package com.njplastic.njplastic_api.audit.filters;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.njplastic.njplastic_api.audit.entities.AuditLog;
import com.njplastic.njplastic_api.audit.services.AuditService;
import com.njplastic.njplastic_api.audit.services.PayloadSanitizer;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.utils.ConstantsAndParams;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Persists one append-only audit_log row per request (RF20, RN12, RNF08).
 * Runs after JwtAuthenticationFilter so the authenticated user is available,
 * and wraps the downstream chain so the final status and bodies (including
 * 401/403 produced by Spring Security) are captured. Bodies are sanitized
 * before persistence; preflight (OPTIONS) and documentation routes are skipped.
 */
@RequiredArgsConstructor
public class AuditFilter extends OncePerRequestFilter {

  private static final int MAX_METHOD_LENGTH = 8;
  private static final int MAX_ENDPOINT_LENGTH = 512;
  private static final int REQUEST_CACHE_LIMIT_BYTES = 65536;

  private final AuditService auditService;
  private final PayloadSanitizer payloadSanitizer;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String uri = request.getRequestURI();
    return uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT_BYTES);
    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
    long startNanos = System.nanoTime();
    try {
      filterChain.doFilter(requestWrapper, responseWrapper);
    } finally {
      int durationMs = (int) ((System.nanoTime() - startNanos) / 1_000_000L);
      persist(requestWrapper, responseWrapper, durationMs);
      responseWrapper.copyBodyToResponse();
    }
  }

  private void persist(ContentCachingRequestWrapper requestWrapper,
      ContentCachingResponseWrapper responseWrapper, int durationMs) {
    AuditLog auditLog = AuditLog.builder()
        .userId(resolveUserId())
        .httpMethod(truncate(requestWrapper.getMethod(), MAX_METHOD_LENGTH))
        .endpoint(truncate(resolveEndpoint(requestWrapper), MAX_ENDPOINT_LENGTH))
        .httpStatus(responseWrapper.getStatus())
        .requestPayload(payloadSanitizer.sanitize(
            requestWrapper.getContentAsByteArray(), requestWrapper.getContentType()))
        .responsePayload(payloadSanitizer.sanitize(
            responseWrapper.getContentAsByteArray(), responseWrapper.getContentType()))
        .sourceIp(resolveSourceIp(requestWrapper))
        .durationMs(durationMs)
        .build();
    auditService.saveAudit(auditLog);
  }

  private UUID resolveUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
      return user.id();
    }
    return null;
  }

  private String resolveEndpoint(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String query = request.getQueryString();
    return StringUtils.hasText(query) ? uri + ConstantsAndParams.SPLIT_REQUEST_PARAMS + query : uri;
  }

  private String resolveSourceIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader(ConstantsAndParams.HTTP_HEADER_FORWARDED_FOR);
    if (StringUtils.hasText(forwardedFor)) {
      return forwardedFor.split(ConstantsAndParams.SPLIT_USER_IP)[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}