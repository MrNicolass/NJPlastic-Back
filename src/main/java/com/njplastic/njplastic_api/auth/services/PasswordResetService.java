package com.njplastic.njplastic_api.auth.services;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.njplastic.njplastic_api.auth.entities.PasswordResetToken;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.exceptions.ExpiredResetTokenException;
import com.njplastic.njplastic_api.auth.exceptions.InvalidResetTokenException;
import com.njplastic.njplastic_api.auth.repositories.PasswordResetTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Orchestrates the password recovery flow (EP-BE-02 reopened). Two entry
 * points: {@link #requestReset(String)} starts the flow and is idempotent
 * - it always succeeds from the caller's perspective whether the login
 * exists or not, preventing enumeration; {@link #confirmReset(String, String)}
 * consumes the opaque token and rotates the BCrypt hash.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
  private static final int TOKEN_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserService userService;
  private final PasswordResetTokenRepository tokenRepository;
  private final EmailService emailService;
  private final EmailProperties emailProperties;
  private final PasswordEncoder passwordEncoder;

  /**
   * Start the recovery flow. Generates a random token, persists it with the
   * configured TTL and dispatches the recovery email. Always silent to the
   * caller - missing login, inactive user or mail-delivery failure are logged
   * but never surfaced (RFC §6.2 / OWASP A07).
   *
   * @param login the login identifier the user typed on the forgot-password screen
   */
  public void requestReset(String login) {
    Optional<User> userOpt = userService.findActiveByLogin(login);
    if (userOpt.isEmpty()) {
      LOGGER.info("Password reset requested for unknown/inactive login");
      return;
    }
    User user = userOpt.get();
    String token = generateToken();
    OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(emailProperties.passwordResetTtlMinutes());
    tokenRepository.save(PasswordResetToken.builder()
        .userId(user.getId())
        .token(token)
        .expiresAt(expiresAt)
        .build());
    try {
      emailService.sendPasswordReset(user.getEmail(), token);
    } catch (RuntimeException ex) {
      LOGGER.warn("Password reset email could not be sent for user [{}]: {}", user.getId(), ex.getMessage());
    }
  }

  /**
   * Finish the recovery flow. Validates the token, rotates the user's password
   * hash and invalidates every outstanding token for the same user.
   *
   * @param token       opaque token from the email
   * @param newPassword the user's chosen new password (plain text)
   * @throws InvalidResetTokenException if the token is unknown
   * @throws ExpiredResetTokenException if the token exists but is past its TTL
   */
  @Transactional
  public void confirmReset(String token, String newPassword) {
    OffsetDateTime now = OffsetDateTime.now();
    PasswordResetToken pending = tokenRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(token, now)
        .orElseThrow(() -> resolveTokenError(token, now));
    String newHash = passwordEncoder.encode(newPassword);
    userService.updatePasswordHash(pending.getUserId(), newHash);
    pending.setUsedAt(now);
    tokenRepository.save(pending);
    tokenRepository.deleteAllByUserId(pending.getUserId());
  }

  private RuntimeException resolveTokenError(String token, OffsetDateTime now) {
    return tokenRepository.findAll().stream()
        .filter(t -> token.equals(t.getToken()))
        .findFirst()
        .map(t -> {
          if (t.getUsedAt() != null || t.getExpiresAt().isBefore(now)) {
            return (RuntimeException) new ExpiredResetTokenException();
          }
          return (RuntimeException) new InvalidResetTokenException();
        })
        .orElseGet(InvalidResetTokenException::new);
  }

  private String generateToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
