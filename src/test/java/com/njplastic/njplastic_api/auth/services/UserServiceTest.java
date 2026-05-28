package com.njplastic.njplastic_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  @Test
  void findActiveByLogin_delegatesToRepository() {
    User user = User.builder().id(UUID.randomUUID()).login("manager").build();
    when(userRepository.findByLoginAndActiveTrue("manager")).thenReturn(Optional.of(user));

    Optional<User> result = userService.findActiveByLogin("manager");

    assertThat(result).containsSame(user);
    verify(userRepository).findByLoginAndActiveTrue("manager");
  }

  @Test
  void findActiveByLogin_returnsEmptyWhenRepositoryEmpty() {
    when(userRepository.findByLoginAndActiveTrue("ghost")).thenReturn(Optional.empty());

    assertThat(userService.findActiveByLogin("ghost")).isEmpty();
  }
}
