package com.njplastic.njplastic_api.auth.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.njplastic.njplastic_api.auth.entities.PasswordResetToken;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.exceptions.ExpiredResetTokenException;
import com.njplastic.njplastic_api.auth.exceptions.InvalidResetTokenException;
import com.njplastic.njplastic_api.auth.repositories.PasswordResetTokenRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock
  private UserService userService;
  @Mock
  private PasswordResetTokenRepository tokenRepository;
  @Mock
  private EmailService emailService;
  @Mock
  private PasswordEncoder passwordEncoder;

  private EmailProperties emailProperties;
  private PasswordResetService service;

  @BeforeEach
  void setUp() {
    emailProperties = new EmailProperties("from@x", "https://reset", 30);
    service = new PasswordResetService(userService, tokenRepository, emailService, emailProperties, passwordEncoder);
  }

  @Test
  void requestReset_silentlyReturnsForUnknownLogin() {
    when(userService.findActiveByLogin("ghost")).thenReturn(Optional.empty());

    assertThatCode(() -> service.requestReset("ghost")).doesNotThrowAnyException();

    verify(tokenRepository, never()).save(any());
    verify(emailService, never()).sendPasswordReset(anyString(), anyString());
  }

  @Test
  void requestReset_persistsTokenAndSendsEmail() {
    User user = User.builder()
        .id(UUID.randomUUID()).login("manager").email("manager@njplastic.com").build();
    when(userService.findActiveByLogin("manager")).thenReturn(Optional.of(user));

    service.requestReset("manager");

    verify(tokenRepository).save(any(PasswordResetToken.class));
    verify(emailService).sendPasswordReset(eq("manager@njplastic.com"), anyString());
  }

  @Test
  void requestReset_swallowsEmailFailure() {
    User user = User.builder()
        .id(UUID.randomUUID()).login("manager").email("manager@njplastic.com").build();
    when(userService.findActiveByLogin("manager")).thenReturn(Optional.of(user));
    doThrow(new RuntimeException("smtp down")).when(emailService).sendPasswordReset(anyString(), anyString());

    assertThatCode(() -> service.requestReset("manager")).doesNotThrowAnyException();
  }

  @Test
  void confirmReset_rotatesPasswordAndInvalidatesTokens() {
    String token = "good-token";
    UUID userId = UUID.randomUUID();
    PasswordResetToken pending = PasswordResetToken.builder()
        .id(UUID.randomUUID())
        .userId(userId)
        .token(token)
        .expiresAt(OffsetDateTime.now().plusMinutes(10))
        .build();
    when(tokenRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(eq(token), any()))
        .thenReturn(Optional.of(pending));
    when(passwordEncoder.encode("new-password-here")).thenReturn("hashed");

    service.confirmReset(token, "new-password-here");

    verify(userService).updatePasswordHash(userId, "hashed");
    verify(tokenRepository).save(pending);
    verify(tokenRepository).deleteAllByUserId(userId);
  }

  @Test
  void confirmReset_throwsInvalidWhenNoMatchAnywhere() {
    when(tokenRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(any(), any()))
        .thenReturn(Optional.empty());
    when(tokenRepository.findAll()).thenReturn(List.of());

    assertThatThrownBy(() -> service.confirmReset("bad", "new-password-here"))
        .isInstanceOf(InvalidResetTokenException.class);
  }

  @Test
  void confirmReset_throwsExpiredWhenTokenAlreadyConsumed() {
    String token = "consumed";
    PasswordResetToken consumed = PasswordResetToken.builder()
        .id(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .token(token)
        .expiresAt(OffsetDateTime.now().plusMinutes(10))
        .usedAt(OffsetDateTime.now())
        .build();
    when(tokenRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(eq(token), any()))
        .thenReturn(Optional.empty());
    when(tokenRepository.findAll()).thenReturn(List.of(consumed));

    assertThatThrownBy(() -> service.confirmReset(token, "new-password-here"))
        .isInstanceOf(ExpiredResetTokenException.class);
  }

  @Test
  void confirmReset_throwsExpiredWhenTokenPastTtl() {
    String token = "expired";
    PasswordResetToken expired = PasswordResetToken.builder()
        .id(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .token(token)
        .expiresAt(OffsetDateTime.now().minusMinutes(1))
        .build();
    when(tokenRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(eq(token), any()))
        .thenReturn(Optional.empty());
    when(tokenRepository.findAll()).thenReturn(List.of(expired));

    assertThatThrownBy(() -> service.confirmReset(token, "new-password-here"))
        .isInstanceOf(ExpiredResetTokenException.class);
  }
}
