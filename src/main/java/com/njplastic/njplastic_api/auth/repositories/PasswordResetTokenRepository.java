package com.njplastic.njplastic_api.auth.repositories;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.auth.entities.PasswordResetToken;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

  Optional<PasswordResetToken> findByTokenAndUsedAtIsNullAndExpiresAtAfter(String token, OffsetDateTime now);

  @Modifying
  @Query("DELETE FROM PasswordResetToken t WHERE t.userId = :userId")
  void deleteAllByUserId(@Param("userId") UUID userId);
}
