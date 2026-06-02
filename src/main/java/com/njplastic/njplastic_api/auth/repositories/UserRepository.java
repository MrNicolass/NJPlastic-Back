package com.njplastic.njplastic_api.auth.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.auth.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

  Optional<User> findByLoginAndActiveTrue(String login);

  boolean existsByLogin(String login);

  boolean existsByEmail(String email);
}
