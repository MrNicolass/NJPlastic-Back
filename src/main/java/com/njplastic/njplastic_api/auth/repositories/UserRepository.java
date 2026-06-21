package com.njplastic.njplastic_api.auth.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

  Optional<User> findByLoginAndActiveTrue(String login);

  boolean existsByLogin(String login);

  boolean existsByEmail(String email);

  List<User> findByActiveTrueAndRoleAndSectorAndShiftOrderByNameAsc(
      UserRole role, String sector, String shift);
}
