package com.njplastic.njplastic_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.njplastic.njplastic_api.auth.dtos.LoginRequestDTO;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.exceptions.InvalidCredentialsException;
import com.njplastic.njplastic_api.auth.security.IssuedToken;
import com.njplastic.njplastic_api.auth.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock
  private UserService userService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtTokenProvider tokenProvider;

  @InjectMocks
  private AuthenticationService authenticationService;

  private User user;
  private LoginRequestDTO request;

  @BeforeEach
  void setUp() {
    user = User.builder()
        .id(UUID.randomUUID())
        .login("manager")
        .name("Manager Default")
        .passwordHash("hash")
        .role(UserRole.MANAGER)
        .build();
    request = LoginRequestDTO.builder().login("manager").password("manager-dev-123").build();
  }

  @Test
  void authenticate_returnsTokenWhenCredentialsValid() {
    IssuedToken issued = new IssuedToken("jwt-token", 1_700_000_000L);
    when(userService.findActiveByLogin("manager")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("manager-dev-123", "hash")).thenReturn(true);
    when(tokenProvider.generate(user)).thenReturn(issued);
    when(tokenProvider.expirationSeconds()).thenReturn(3600L);

    AuthenticationResult result = authenticationService.authenticate(request);

    assertThat(result.response().getToken()).isEqualTo("jwt-token");
    assertThat(result.response().getTokenType()).isEqualTo("Bearer");
    assertThat(result.response().getExpiresInSeconds()).isEqualTo(3600L);
    assertThat(result.response().getUser().getLogin()).isEqualTo("manager");
    assertThat(result.issued()).isSameAs(issued);
  }

  @Test
  void authenticate_throwsWhenUserNotFound() {
    when(userService.findActiveByLogin("manager")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authenticationService.authenticate(request))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(tokenProvider, never()).generate(any());
  }

  @Test
  void authenticate_runsDummyHashComparisonWhenUserNotFound() {
    when(userService.findActiveByLogin("manager")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authenticationService.authenticate(request))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(passwordEncoder).matches(eq("manager-dev-123"), any(String.class));
  }

  @Test
  void authenticate_throwsWhenPasswordDoesNotMatch() {
    when(userService.findActiveByLogin("manager")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("manager-dev-123", "hash")).thenReturn(false);

    assertThatThrownBy(() -> authenticationService.authenticate(request))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(tokenProvider, never()).generate(any());
  }
}
