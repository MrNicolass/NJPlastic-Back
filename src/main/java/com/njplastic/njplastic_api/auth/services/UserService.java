package com.njplastic.njplastic_api.auth.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link UserRepository}. Other domains must read or write user
 * data through this service rather than injecting the repository directly, so
 * the user aggregate keeps a single point of access.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  /**
   * Look up an active user by login.
   *
   * @param login the login identifier
   * @return the matching active user, or empty if none exists or it is inactive
   */
  public Optional<User> findActiveByLogin(String login) {
    return userRepository.findByLoginAndActiveTrue(login);
  }
}
