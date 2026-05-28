package com.njplastic.njplastic_api.auth.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.auth.dtos.LoginRequest;
import com.njplastic.njplastic_api.auth.dtos.LoginResponse;
import com.njplastic.njplastic_api.auth.dtos.UserSummary;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.exceptions.InvalidCredentialsException;
import com.njplastic.njplastic_api.auth.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

/**
 * Validates credentials and issues JWTs. Always throws
 * {@link InvalidCredentialsException} - whether the login does not exist,
 * the user is inactive, or the password does not match - aligned with RFC
 * §3.2.1 and OWASP A07 to prevent user enumeration.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;

  public LoginResponse authenticate(LoginRequest request) {
    User user = userService.findActiveByLogin(request.getLogin())
        .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    String token = tokenProvider.generate(user);
    return LoginResponse.builder()
        .token(token)
        .tokenType("Bearer")
        .expiresInSeconds(tokenProvider.expirationSeconds())
        .user(UserSummary.from(user))
        .build();
  }
}