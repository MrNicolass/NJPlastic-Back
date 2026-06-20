package com.njplastic.njplastic_api.auth.services;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.exceptions.UserNotFoundException;
import com.njplastic.njplastic_api.auth.repositories.UserRepository;

import jakarta.persistence.criteria.Predicate;
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

 /**
 * Look up a user by primary key, regardless of {@code active} flag. Returns
 * the entity verbatim so admin flows can present soft-deleted users.
 *
 * @param id user UUID
 * @return the matching user, or empty
 */
  public Optional<User> findById(UUID id) {
    return userRepository.findById(id);
  }

 /**
 * Read-side projection used by the admin {@code GET /users} endpoint.
 *
 * @param role optional role filter
 * @param sector optional sector filter (exact match)
 * @param shift optional shift filter (exact match)
 * @param active optional active flag filter
 * @param pageable paging/sort
 * @return paginated users matching every supplied filter
 */
  public Page<User> findPaged(UserRole role, String sector, String shift, Boolean active, Pageable pageable) {
    Specification<User> spec = (root, query, cb) -> {
      Predicate predicate = cb.conjunction();
      if (role != null) {
        predicate = cb.and(predicate, cb.equal(root.get("role"), role));
      }
      if (sector != null && !sector.isBlank()) {
        predicate = cb.and(predicate, cb.equal(root.get("sector"), sector));
      }
      if (shift != null && !shift.isBlank()) {
        predicate = cb.and(predicate, cb.equal(root.get("shift"), shift));
      }
      if (active != null) {
        predicate = cb.and(predicate, cb.equal(root.get("active"), active));
      }
      return predicate;
    };
    return userRepository.findAll(spec, pageable);
  }

 /**
 * Persist a brand-new user. Caller is responsible for hashing the password
 * before invocation; the service only enforces uniqueness on the login.
 *
 * @param user fully populated entity
 * @return the persisted entity (with id/timestamps populated)
 */
  @Transactional
  public User create(User user) {
    return userRepository.save(user);
  }

 /**
 * Apply administrative changes to an existing user. The password hash is
 * deliberately not editable here - it rotates only through the password
 * reset flow ({@link PasswordResetService}).
 *
 * @param id user UUID
 * @param updated entity carrying the fields to apply
 * @return the persisted entity after the update
 * @throws UserNotFoundException if no user matches {@code id}
 */
  @Transactional
  public User update(UUID id, User updated) {
    User current = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    current.setName(updated.getName());
    current.setEmail(updated.getEmail());
    current.setRole(updated.getRole());
    current.setSector(updated.getSector());
    current.setShift(updated.getShift());
    current.setActive(updated.isActive());
    return userRepository.save(current);
  }

 /**
 * Mark the user as inactive. Soft-delete preserves the FK semantic in
 * {@code audit_log.user_id} (no physical REFERENCES) while
 * removing the account from active queries.
 *
 * @param id user UUID
 * @throws UserNotFoundException if no user matches {@code id}
 */
  @Transactional
  public void softDelete(UUID id) {
    User current = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    current.setActive(false);
    current.setUpdatedAt(OffsetDateTime.now());
    userRepository.save(current);
  }

 /**
 * Rotate the BCrypt hash for the given user. Called by the password reset
 * confirm flow after the token has been validated.
 *
 * @param id user UUID
 * @param newHash already-encoded BCrypt hash
 * @throws UserNotFoundException if no user matches {@code id}
 */
  @Transactional
  public void updatePasswordHash(UUID id, String newHash) {
    User current = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    current.setPasswordHash(newHash);
    current.setUpdatedAt(OffsetDateTime.now());
    userRepository.save(current);
  }

 /**
 * @param login login identifier
 * @return true if any row in {@code users} (active or not) already uses the login
 */
  public boolean existsByLogin(String login) {
    return userRepository.existsByLogin(login);
  }

 /**
 * @param email contact email
 * @return true if any row in {@code users} (active or not) already uses the email
 */
  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }
}
