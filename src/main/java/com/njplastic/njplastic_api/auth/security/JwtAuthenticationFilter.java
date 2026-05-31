package com.njplastic.njplastic_api.auth.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.njplastic.njplastic_api.utils.ConstantsAndParams;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Reads "Authorization: Bearer <token>" on every request, validates the JWT
 * via JwtTokenProvider and populates SecurityContextHolder. Anonymous routes
 * (login, swagger, versioning) are filtered through unchanged - access is
 * decided by SecurityConfig's authorizeHttpRequests.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(ConstantsAndParams.HTTP_BEARER_PREFIX)) {
      String token = header.substring(ConstantsAndParams.HTTP_BEARER_PREFIX.length()).trim();
      Optional<Claims> claims = tokenProvider.parse(token);
      claims.flatMap(tokenProvider::toAuthenticatedUser)
          .ifPresent(this::populateSecurityContext);
    }
    filterChain.doFilter(request, response);
  }

  private void populateSecurityContext(AuthenticatedUser user) {
    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        user,
        null,
        List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}