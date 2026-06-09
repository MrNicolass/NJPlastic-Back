package com.njplastic.njplastic_api.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.njplastic.njplastic_api.audit.filters.AuditFilter;
import com.njplastic.njplastic_api.audit.services.AuditService;
import com.njplastic.njplastic_api.audit.services.PayloadSanitizer;
import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;
import com.njplastic.njplastic_api.utils.ConstantsAndParams;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Application security wiring (EP-BE-02 / RFC §6.2). Stateless JWT chain,
 * CSRF disabled, CORS restricted to the configured frontend origins.
 *
 * Authorization mapping inherited by downstream epics (RN01..RN04):
 * <ul>
 * <li>RN01 (authenticated only) - covered globally by authenticated()</li>
 * <li>RN02 (operator scope)
 * - @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')") + sector/shift
 * filter in the service</li>
 * <li>RN03 (leader scope) - @PreAuthorize("hasAnyRole('LEADER','MANAGER')") +
 * sector filter in the service</li>
 * <li>RN04 (manager scope) - @PreAuthorize("hasRole('MANAGER')") for
 * writes/admin endpoints</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({ JwtProperties.class, CorsProperties.class, SecurityProperties.class,
    CookieProperties.class })
public class SecurityConfig {

  private final ObjectMapper objectMapper;

  public SecurityConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  JwtTokenProvider jwtTokenProvider(JwtProperties properties) {
    return new JwtTokenProvider(properties);
  }

  @Bean
  JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    return new JwtAuthenticationFilter(jwtTokenProvider);
  }

  @Bean
  AuditFilter auditFilter(AuditService auditService, PayloadSanitizer payloadSanitizer) {
    return new AuditFilter(auditService, payloadSanitizer);
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      AuditFilter auditFilter,
      CorsConfigurationSource corsConfigurationSource,
      SecurityProperties securityProperties) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(securityProperties.publicPaths().toArray(String[]::new)).permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(auditFilter, JwtAuthenticationFilter.class)
        .exceptionHandling(eh -> eh
            .authenticationEntryPoint(this::writeUnauthorized)
            .accessDeniedHandler(this::writeForbidden))
        .headers(h -> h
            .contentTypeOptions(c -> {
            })
            .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true)));
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration cfg = new CorsConfiguration();
    List<String> origins = properties.allowedOrigins();
    if (origins != null && !origins.isEmpty()) {
      cfg.setAllowedOrigins(origins);
    }
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Request-Id"));
    cfg.setExposedHeaders(List.of("Authorization"));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }

  private void writeUnauthorized(HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.core.AuthenticationException ex) throws IOException {
    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid Credentials",
        ex.getClass().getSimpleName());
  }

  private void writeForbidden(HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException ex) throws IOException {
    writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access Denied",
        ex.getClass().getSimpleName());
  }

  private void writeError(HttpServletResponse response, int status, String message, String clazzError)
      throws IOException {
    ErrorResponseDTO body = ErrorResponseDTO.builder()
        .message(message)
        .clazzError(clazzError)
        .build();
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(ConstantsAndParams.ENCODER_TEXT);
    objectMapper.writeValue(response.getWriter(), body);
  }
}