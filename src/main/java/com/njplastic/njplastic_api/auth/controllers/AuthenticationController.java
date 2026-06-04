package com.njplastic.njplastic_api.auth.controllers;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.njplastic.njplastic_api.auth.dtos.LoginRequestDTO;
import com.njplastic.njplastic_api.auth.dtos.LoginResponseDTO;
import com.njplastic.njplastic_api.auth.dtos.PasswordResetConfirmDTO;
import com.njplastic.njplastic_api.auth.dtos.PasswordResetRequestDTO;
import com.njplastic.njplastic_api.auth.dtos.RefreshResponseDTO;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.auth.security.CookieProperties;
import com.njplastic.njplastic_api.auth.security.IssuedToken;
import com.njplastic.njplastic_api.auth.security.JwtTokenProvider;
import com.njplastic.njplastic_api.auth.services.AuthenticationResult;
import com.njplastic.njplastic_api.auth.services.AuthenticationService;
import com.njplastic.njplastic_api.auth.services.PasswordResetService;
import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "JWT login, refresh and password reset endpoints (RFC §6.2 / EP-BE-02)")
@RequiredArgsConstructor
public class AuthenticationController {

  private static final String COOKIE_ACCESS_TOKEN = "access_token";
  private static final String COOKIE_ACCESS_TOKEN_EXP = "access_token_exp";
  private static final String COOKIE_SAME_SITE = "Strict";
  private static final String COOKIE_PATH = "/";

  private final AuthenticationService authenticationService;
  private final PasswordResetService passwordResetService;
  private final JwtTokenProvider tokenProvider;
  private final CookieProperties cookieProperties;

  @PostMapping("/login")
  @SecurityRequirements({})
  @Operation(summary = "Authenticate and obtain a JWT", description = "Validates credentials against the users table and returns a stateless HS256 JWT. "
      + "Also emits the dual cookies access_token (httpOnly) and access_token_exp (JS-readable, exp UNIX) "
      + "so the Next.js frontend can drive proactive refresh (EP-FE-02). "
      + "On failure, returns the same generic 401 response regardless of whether the login exists "
      + "or the password is wrong (OWASP A07).")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Authentication succeeded", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request, HttpServletResponse response) {
    AuthenticationResult result = authenticationService.authenticate(request);
    writeAuthCookies(response, result.issued());
    return result.response();
  }

  @PostMapping("/refresh")
  @Operation(summary = "Refresh an active JWT", description = "Re-issues a JWT carrying the same claims as the current one with a new exp and "
      + "re-emits the access_token/access_token_exp cookies. Requires a valid token on the Authorization "
      + "header or the access_token cookie; the frontend dispatches this a few minutes before exp.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Token refreshed", content = @Content(schema = @Schema(implementation = RefreshResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public RefreshResponseDTO refresh(@AuthenticationPrincipal AuthenticatedUser principal, HttpServletResponse response) {
    IssuedToken issued = tokenProvider.refresh(principal);
    writeAuthCookies(response, issued);
    return RefreshResponseDTO.builder()
        .token(issued.compact())
        .tokenType("Bearer")
        .expiresInSeconds(tokenProvider.expirationSeconds())
        .build();
  }

  @PostMapping("/password-reset")
  @SecurityRequirements({})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Start password recovery", description = "Idempotent endpoint: always returns 204, whether the login exists or not. "
      + "If a matching active user is found, an email with a single-use opaque token is dispatched (OWASP A07).")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Request accepted"),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public void requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO request) {
    passwordResetService.requestReset(request.getLogin());
  }

  @PostMapping("/password-reset/confirm")
  @SecurityRequirements({})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Finish password recovery", description = "Validates the opaque token, rotates the user's BCrypt hash and invalidates every "
      + "outstanding token for the same user.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Password updated"),
      @ApiResponse(responseCode = "400", description = "Invalid or expired token / malformed payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDTO request) {
    passwordResetService.confirmReset(request.getToken(), request.getNewPassword());
  }

  /**
   * Emit the dual cookies that back the EP-FE-02 authentication contract:
   * {@code access_token} (httpOnly) for browser-side auth and
   * {@code access_token_exp} (JS-readable) so the axios interceptor can
   * dispatch {@code /auth/refresh} proactively. Uses {@code addHeader} so the
   * two Set-Cookie headers coexist on the response.
   */
  private void writeAuthCookies(HttpServletResponse response, IssuedToken issued) {
    Duration maxAge = Duration.ofSeconds(tokenProvider.expirationSeconds());
    boolean secure = cookieProperties.secure();

    ResponseCookie accessToken = ResponseCookie.from(COOKIE_ACCESS_TOKEN, issued.compact())
        .httpOnly(true)
        .secure(secure)
        .sameSite(COOKIE_SAME_SITE)
        .path(COOKIE_PATH)
        .maxAge(maxAge)
        .build();
    ResponseCookie accessTokenExp = ResponseCookie.from(COOKIE_ACCESS_TOKEN_EXP, Long.toString(issued.expEpochSeconds()))
        .httpOnly(false)
        .secure(secure)
        .sameSite(COOKIE_SAME_SITE)
        .path(COOKIE_PATH)
        .maxAge(maxAge)
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, accessToken.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, accessTokenExp.toString());
  }
}
