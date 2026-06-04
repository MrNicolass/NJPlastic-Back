package com.njplastic.njplastic_api.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates HS256 JWTs (RFC §6.2 / RFC 7519). The signing key is
 * loaded once at construction; tokens with "alg=none" or any algorithm other
 * than HS256 are rejected by jjwt's strict parser (OWASP A02).
 */
public class JwtTokenProvider {

  private static final String CLAIM_ROLE = "role";
  private static final String CLAIM_SECTOR = "sector";
  private static final String CLAIM_SHIFT = "shift";
  private static final int MIN_SECRET_LENGTH = 64;

  private final SecretKey signingKey;
  private final long expirationMinutes;
  private final String issuer;

  public JwtTokenProvider(JwtProperties properties) {
    String secret = properties.secret();
    if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException(
          "app.security.jwt.secret must be at least " + MIN_SECRET_LENGTH
              + " characters. Set the JWT_SECRET environment variable.");
    }
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMinutes = properties.expirationMinutes();
    this.issuer = properties.issuer();
  }

  /**
   * Generate an HS256 JWT carrying the user identity and authorization scope.
   * Returns the compact serialization together with the same {@code exp}
   * (UNIX epoch seconds) baked into the token, so the caller can mirror it to
   * the {@code access_token_exp} cookie without re-parsing.
   */
  public IssuedToken generate(User user) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(expirationMinutes * 60);
    String compact = Jwts.builder()
        .issuer(issuer)
        .subject(user.getId().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .claim(CLAIM_ROLE, user.getRole().name())
        .claim(CLAIM_SECTOR, user.getSector())
        .claim(CLAIM_SHIFT, user.getShift())
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
    return new IssuedToken(compact, exp.getEpochSecond());
  }

  /**
   * Parse and validate a compact JWT. Returns Optional.empty() for any
   * signature/format/expiration failure. Callers never see the underlying
   * exception type, preventing leakage of which check failed.
   */
  public Optional<Claims> parse(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(signingKey)
          .requireIssuer(issuer)
          .build()
          .parseSignedClaims(token)
          .getPayload();
      return Optional.of(claims);
    } catch (JwtException | IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  /**
   * Lift a validated Claims set into the principal stored in the
   * SecurityContext. The role claim is required; missing or unknown values
   * make the token unusable.
   */
  public Optional<AuthenticatedUser> toAuthenticatedUser(Claims claims) {
    try {
      UUID id = UUID.fromString(claims.getSubject());
      UserRole role = UserRole.valueOf(claims.get(CLAIM_ROLE, String.class));
      String sector = claims.get(CLAIM_SECTOR, String.class);
      String shift = claims.get(CLAIM_SHIFT, String.class);
      return Optional.of(new AuthenticatedUser(id, null, role, sector, shift));
    } catch (IllegalArgumentException | NullPointerException ex) {
      return Optional.empty();
    }
  }

  /**
   * Issue a fresh token carrying the same claims as the supplied principal.
   * Used by {@code POST /auth/refresh} (EP-BE-02 reopened) - the frontend
   * dispatches this ~5 min before {@code exp}, keeping the user signed in
   * without forcing a re-login or holding a separate refresh-token store.
   *
   * @param principal authenticated user resolved from the current JWT
   * @return new compact HS256 token paired with its UNIX exp
   */
  public IssuedToken refresh(AuthenticatedUser principal) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(expirationMinutes * 60);
    String compact = Jwts.builder()
        .issuer(issuer)
        .subject(principal.id().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .claim(CLAIM_ROLE, principal.role().name())
        .claim(CLAIM_SECTOR, principal.sector())
        .claim(CLAIM_SHIFT, principal.shift())
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
    return new IssuedToken(compact, exp.getEpochSecond());
  }

  public long expirationSeconds() {
    return expirationMinutes * 60;
  }
}