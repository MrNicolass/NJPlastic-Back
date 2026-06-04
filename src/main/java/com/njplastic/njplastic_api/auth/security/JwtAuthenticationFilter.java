package com.njplastic.njplastic_api.auth.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.njplastic.njplastic_api.utils.ConstantsAndParams;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Validates the JWT on every request and populates SecurityContextHolder. The
 * token is read from "Authorization: Bearer <token>" first, falling back to
 * the {@code access_token} httpOnly cookie emitted by /auth/login (EP-FE-02
 * dual-cookie contract). Anonymous routes (login, swagger, versioning) are
 * filtered through unchanged - access is decided by SecurityConfig's
 * authorizeHttpRequests.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String COOKIE_ACCESS_TOKEN = "access_token";

  private final JwtTokenProvider tokenProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    resolveToken(request)
        .flatMap(tokenProvider::parse)
        .flatMap(tokenProvider::toAuthenticatedUser)
        .ifPresent(this::populateSecurityContext);
    filterChain.doFilter(request, response);
  }

  private Optional<String> resolveToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(ConstantsAndParams.HTTP_BEARER_PREFIX)) {
      String token = header.substring(ConstantsAndParams.HTTP_BEARER_PREFIX.length()).trim();
      if (!token.isEmpty()) {
        return Optional.of(token);
      }
    }
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    return Arrays.stream(cookies)
        .filter(c -> COOKIE_ACCESS_TOKEN.equals(c.getName()))
        .map(Cookie::getValue)
        .filter(v -> v != null && !v.isEmpty())
        .findFirst();
  }

  private void populateSecurityContext(AuthenticatedUser user) {
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        user,
        null,
        List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}