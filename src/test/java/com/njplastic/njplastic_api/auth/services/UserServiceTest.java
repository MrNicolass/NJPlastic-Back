package com.njplastic.njplastic_api.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.exceptions.UserNotFoundException;
import com.njplastic.njplastic_api.auth.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  private User sampleUser() {
    return User.builder()
        .id(UUID.randomUUID())
        .login("manager")
        .name("Manager")
        .email("manager@njplastic.com")
        .role(UserRole.MANAGER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .active(true)
        .build();
  }

  @Test
  void findActiveByLogin_delegatesToRepository() {
    User user = sampleUser();
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

  @Test
  void findById_delegatesToRepository() {
    UUID id = UUID.randomUUID();
    User user = sampleUser();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    assertThat(userService.findById(id)).containsSame(user);
  }

  @Test
  void findPaged_delegatesToRepositoryWithSpec() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<User> page = new PageImpl<>(List.of(sampleUser()));
    when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    Page<User> result = userService.findPaged(UserRole.MANAGER, "INJECAO", "TURNO_A", true, pageable);

    assertThat(result).isSameAs(page);
    verify(userRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void findPaged_acceptsNullFilters() {
    Pageable pageable = PageRequest.of(0, 10);
    when(userRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of()));

    userService.findPaged(null, null, null, null, pageable);

    verify(userRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void create_savesEntity() {
    User user = sampleUser();
    when(userRepository.save(user)).thenReturn(user);

    assertThat(userService.create(user)).isSameAs(user);
    verify(userRepository).save(user);
  }

  @Test
  void update_appliesPatchAndSaves() {
    UUID id = UUID.randomUUID();
    User current = sampleUser();
    current.setId(id);
    User patch = User.builder()
        .name("New Name")
        .email("new@njplastic.com")
        .role(UserRole.LEADER)
        .sector("ACABAMENTO")
        .shift("TURNO_B")
        .active(false)
        .build();
    when(userRepository.findById(id)).thenReturn(Optional.of(current));
    when(userRepository.save(current)).thenReturn(current);

    User result = userService.update(id, patch);

    assertThat(result.getName()).isEqualTo("New Name");
    assertThat(result.getEmail()).isEqualTo("new@njplastic.com");
    assertThat(result.getRole()).isEqualTo(UserRole.LEADER);
    assertThat(result.getSector()).isEqualTo("ACABAMENTO");
    assertThat(result.getShift()).isEqualTo("TURNO_B");
    assertThat(result.isActive()).isFalse();
  }

  @Test
  void update_throwsWhenUserMissing() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.update(id, new User()))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void softDelete_marksInactiveAndSaves() {
    UUID id = UUID.randomUUID();
    User current = sampleUser();
    current.setId(id);
    current.setActive(true);
    when(userRepository.findById(id)).thenReturn(Optional.of(current));

    userService.softDelete(id);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().isActive()).isFalse();
    assertThat(captor.getValue().getUpdatedAt()).isNotNull();
  }

  @Test
  void softDelete_throwsWhenUserMissing() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.softDelete(id))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void updatePasswordHash_rotatesHashAndSaves() {
    UUID id = UUID.randomUUID();
    User current = sampleUser();
    current.setId(id);
    when(userRepository.findById(id)).thenReturn(Optional.of(current));

    userService.updatePasswordHash(id, "new-hash");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getPasswordHash()).isEqualTo("new-hash");
    assertThat(captor.getValue().getUpdatedAt()).isNotNull();
  }

  @Test
  void updatePasswordHash_throwsWhenUserMissing() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updatePasswordHash(id, "x"))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void existsByLogin_delegates() {
    when(userRepository.existsByLogin("manager")).thenReturn(true);
    assertThat(userService.existsByLogin("manager")).isTrue();
  }

  @Test
  void existsByEmail_delegates() {
    when(userRepository.existsByEmail("manager@njplastic.com")).thenReturn(false);
    assertThat(userService.existsByEmail("manager@njplastic.com")).isFalse();
  }
}
