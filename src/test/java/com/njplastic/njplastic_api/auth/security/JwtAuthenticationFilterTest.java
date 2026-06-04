package com.njplastic.njplastic_api.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.njplastic.njplastic_api.auth.enums.UserRole;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock
  private JwtTokenProvider tokenProvider;

  @InjectMocks
  private JwtAuthenticationFilter filter;

  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    chain = mock(FilterChain.class);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void populatesContextForValidBearerToken() throws Exception {
    AuthenticatedUser principal = new AuthenticatedUser(
        UUID.randomUUID(), null, UserRole.MANAGER, null, null);
    Claims claims = mock(Claims.class);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer good-token");
    when(tokenProvider.parse("good-token")).thenReturn(Optional.of(claims));
    when(tokenProvider.toAuthenticatedUser(claims)).thenReturn(Optional.of(principal));

    filter.doFilterInternal(request, response, chain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isEqualTo(principal);
    assertThat(authentication.getAuthorities())
        .extracting("authority").containsExactly("ROLE_MANAGER");
    verify(chain).doFilter(request, response);
  }

  @Test
  void doesNotPopulateContextWhenNoHeaderAndNoCookies() throws Exception {
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
    when(request.getCookies()).thenReturn(null);

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  void doesNotPopulateContextWhenNotBearerAndNoCookies() throws Exception {
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic abc");
    when(request.getCookies()).thenReturn(null);

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  void doesNotPopulateContextWhenTokenInvalid() throws Exception {
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bad-token");
    when(tokenProvider.parse("bad-token")).thenReturn(Optional.empty());

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(tokenProvider, org.mockito.Mockito.never()).toAuthenticatedUser(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void populatesContextFromAccessTokenCookieWhenHeaderMissing() throws Exception {
    AuthenticatedUser principal = new AuthenticatedUser(
        UUID.randomUUID(), null, UserRole.OPERATOR, "INJECAO", "TURNO_A");
    Claims claims = mock(Claims.class);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
    when(request.getCookies()).thenReturn(new Cookie[] {
        new Cookie("other_cookie", "ignored"),
        new Cookie("access_token", "cookie-token")
    });
    when(tokenProvider.parse("cookie-token")).thenReturn(Optional.of(claims));
    when(tokenProvider.toAuthenticatedUser(claims)).thenReturn(Optional.of(principal));

    filter.doFilterInternal(request, response, chain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isEqualTo(principal);
    assertThat(authentication.getAuthorities())
        .extracting("authority").containsExactly("ROLE_OPERATOR");
    verify(chain).doFilter(request, response);
  }

  @Test
  void headerWinsOverCookieWhenBothPresent() throws Exception {
    AuthenticatedUser principal = new AuthenticatedUser(
        UUID.randomUUID(), null, UserRole.MANAGER, null, null);
    Claims claims = mock(Claims.class);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer header-token");
    when(tokenProvider.parse("header-token")).thenReturn(Optional.of(claims));
    when(tokenProvider.toAuthenticatedUser(claims)).thenReturn(Optional.of(principal));

    filter.doFilterInternal(request, response, chain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isEqualTo(principal);
    verify(tokenProvider, org.mockito.Mockito.never()).parse("cookie-token");
    verify(chain).doFilter(request, response);
  }

  @Test
  void doesNotPopulateContextWhenCookieValueInvalid() throws Exception {
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
    when(request.getCookies()).thenReturn(new Cookie[] {
        new Cookie("access_token", "bad-cookie-token")
    });
    when(tokenProvider.parse("bad-cookie-token")).thenReturn(Optional.empty());

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(tokenProvider, org.mockito.Mockito.never()).toAuthenticatedUser(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void doesNotPopulateContextWhenAccessTokenCookieAbsent() throws Exception {
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
    when(request.getCookies()).thenReturn(new Cookie[] {
        new Cookie("session", "value"),
        new Cookie("access_token_exp", "1700000000")
    });

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(tokenProvider, org.mockito.Mockito.never()).parse(any());
    verify(chain).doFilter(request, response);
  }
}
