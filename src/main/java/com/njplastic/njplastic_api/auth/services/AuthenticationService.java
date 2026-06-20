package com.njplastic.njplastic_api.auth.services;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.auth.dtos.LoginRequestDTO;
import com.njplastic.njplastic_api.auth.dtos.LoginResponseDTO;
import com.njplastic.njplastic_api.auth.dtos.UserSummaryDTO;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.exceptions.InvalidCredentialsException;
import com.njplastic.njplastic_api.auth.security.IssuedToken;
import com.njplastic.njplastic_api.auth.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

/**
 * Validates credentials and issues JWTs. Always throws
 * {@link InvalidCredentialsException} - whether the login does not exist,
 * the user is inactive, or the password does not match - aligned with RFC
 * OWASP A07 to prevent user enumeration.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private static final String DUMMY_HASH = "$2a$12$ehZuvVATxi/fCBcEXmCrEOWiw63ibDCmbzFFzFPs2VS2ci6Mjj7xO";

  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;

 /**
 * Authenticate a login request and issue a JWT. A BCrypt comparison is always
 * performed - against the stored hash when the user exists, or against a fixed
 * dummy hash otherwise - so missing/inactive logins take the same time as wrong
 * passwords, preventing user enumeration via timing (OWASP A07).
 *
 * @param request the login credentials
 * @return the JSON response paired with the issued token (compact + exp) so
 * the controller can mirror the exp into the {@code access_token_exp}
 * cookie alongside the {@code access_token} httpOnly cookie
 */
  public AuthenticationResult authenticate(LoginRequestDTO request) {
    Optional<User> userOpt = userService.findActiveByLogin(request.getLogin());
    String hash = userOpt.map(User::getPasswordHash).orElse(DUMMY_HASH);
    boolean matches = passwordEncoder.matches(request.getPassword(), hash);

    if (userOpt.isEmpty() || !matches) {
      throw new InvalidCredentialsException();
    }

    User user = userOpt.get();
    IssuedToken issued = tokenProvider.generate(user);
    LoginResponseDTO response = LoginResponseDTO.builder()
        .token(issued.compact())
        .tokenType("Bearer")
        .expiresInSeconds(tokenProvider.expirationSeconds())
        .user(UserSummaryDTO.from(user))
        .build();
    return new AuthenticationResult(response, issued);
  }
}