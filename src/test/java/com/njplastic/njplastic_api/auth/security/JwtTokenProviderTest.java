package com.njplastic.njplastic_api.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;

import io.jsonwebtoken.Claims;

class JwtTokenProviderTest {

  private static final String SECRET =
      "test-secret-only-used-by-junit-do-not-deploy-test-secret-only-used-by-junit";
  private static final String ISSUER = "NJPlastic-Test";

  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    provider = new JwtTokenProvider(new JwtProperties(SECRET, 60, ISSUER));
  }

  private User user() {
    return User.builder()
        .id(UUID.randomUUID())
        .role(UserRole.LEADER)
        .sector("INJECAO")
        .shift("TURNO_A")
        .build();
  }

  @Test
  void constructor_rejectsNullSecret() {
    assertThatThrownBy(() -> new JwtTokenProvider(new JwtProperties(null, 60, ISSUER)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void constructor_rejectsShortSecret() {
    assertThatThrownBy(() -> new JwtTokenProvider(new JwtProperties("tooshort", 60, ISSUER)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void generateThenParse_roundTripsClaims() {
    User user = user();
    String token = provider.generate(user);

    Optional<Claims> parsed = provider.parse(token);
    assertThat(parsed).isPresent();
    Claims claims = parsed.get();
    assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
    assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    assertThat(claims.get("role", String.class)).isEqualTo("LEADER");
    assertThat(claims.get("sector", String.class)).isEqualTo("INJECAO");
    assertThat(claims.get("shift", String.class)).isEqualTo("TURNO_A");
  }

  @Test
  void parse_returnsEmptyForGarbage() {
    assertThat(provider.parse("not-a-jwt")).isEmpty();
  }

  @Test
  void parse_returnsEmptyForWrongIssuer() {
    JwtTokenProvider other = new JwtTokenProvider(new JwtProperties(SECRET, 60, "OtherIssuer"));
    String token = other.generate(user());
    assertThat(provider.parse(token)).isEmpty();
  }

  @Test
  void parse_returnsEmptyForWrongSigningKey() {
    JwtTokenProvider other = new JwtTokenProvider(
        new JwtProperties(SECRET.replace('t', 'x'), 60, ISSUER));
    String token = other.generate(user());
    assertThat(provider.parse(token)).isEmpty();
  }

  @Test
  void toAuthenticatedUser_mapsValidClaims() {
    User user = user();
    Claims claims = provider.parse(provider.generate(user)).orElseThrow();

    Optional<AuthenticatedUser> result = provider.toAuthenticatedUser(claims);
    assertThat(result).isPresent();
    AuthenticatedUser principal = result.get();
    assertThat(principal.id()).isEqualTo(user.getId());
    assertThat(principal.role()).isEqualTo(UserRole.LEADER);
    assertThat(principal.sector()).isEqualTo("INJECAO");
    assertThat(principal.shift()).isEqualTo("TURNO_A");
    assertThat(principal.login()).isNull();
  }

  @Test
  void toAuthenticatedUser_returnsEmptyForBadSubject() {
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn("not-a-uuid");

    assertThat(provider.toAuthenticatedUser(claims)).isEmpty();
  }

  @Test
  void toAuthenticatedUser_returnsEmptyForUnknownRole() {
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
    when(claims.get("role", String.class)).thenReturn("ADMIN");

    assertThat(provider.toAuthenticatedUser(claims)).isEmpty();
  }

  @Test
  void expirationSeconds_isMinutesTimesSixty() {
    assertThat(provider.expirationSeconds()).isEqualTo(3600L);
  }

  @Test
  void refresh_emitsTokenWithSamePrincipalClaims() {
    User user = user();
    Claims original = provider.parse(provider.generate(user)).orElseThrow();
    AuthenticatedUser principal = provider.toAuthenticatedUser(original).orElseThrow();

    String refreshed = provider.refresh(principal);

    Claims claims = provider.parse(refreshed).orElseThrow();
    assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
    assertThat(claims.get("role", String.class)).isEqualTo("LEADER");
    assertThat(claims.get("sector", String.class)).isEqualTo("INJECAO");
    assertThat(claims.get("shift", String.class)).isEqualTo("TURNO_A");
    assertThat(claims.getIssuer()).isEqualTo(ISSUER);
  }
}
