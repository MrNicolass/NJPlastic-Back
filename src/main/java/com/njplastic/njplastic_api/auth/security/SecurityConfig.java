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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Application security wiring (EP-BE-02 / RFC §6.2). Stateless JWT chain,
 * CSRF disabled, CORS restricted to the configured frontend origins.
 *
 * Authorization mapping inherited by downstream epics (RN01..RN04):
 * <ul>
 *   <li>RN01 (authenticated only) - covered globally by authenticated()</li>
 *   <li>RN02 (operator scope)     - @PreAuthorize("hasAnyRole('OPERATOR','LEADER','MANAGER')") + sector/shift filter in the service</li>
 *   <li>RN03 (leader scope)       - @PreAuthorize("hasAnyRole('LEADER','MANAGER')") + sector filter in the service</li>
 *   <li>RN04 (manager scope)      - @PreAuthorize("hasRole('MANAGER')") for writes/admin endpoints</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({ JwtProperties.class, CorsProperties.class })
public class SecurityConfig {

  private static final String[] PUBLIC_PATHS = {
      "/auth/login",
      "/swagger-ui.html",
      "/swagger-ui/**",
      "/v3/api-docs",
      "/v3/api-docs/**",
      "/api/v1/versioning",
      "/api/v1/versioning/**"
  };

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
  SecurityFilterChain securityFilterChain(HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      CorsConfigurationSource corsConfigurationSource) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(PUBLIC_PATHS).permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(eh -> eh
            .authenticationEntryPoint(this::writeUnauthorized)
            .accessDeniedHandler((req, res, ex) -> writeJson(res, HttpServletResponse.SC_FORBIDDEN,
                "{\"error\":\"Acesso negado\"}")))
        .headers(h -> h
            .contentTypeOptions(c -> {})
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
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    cfg.setExposedHeaders(List.of("Authorization"));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }

  private void writeUnauthorized(jakarta.servlet.http.HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.core.AuthenticationException ex) throws IOException {
    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "{\"error\":\"Credenciais inválidas\"}");
  }

  private void writeJson(HttpServletResponse response, int status, String body) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(body);
  }
}
