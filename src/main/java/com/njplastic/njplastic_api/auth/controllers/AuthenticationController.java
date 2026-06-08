package com.njplastic.njplastic_api.auth.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.njplastic.njplastic_api.auth.dtos.UserSummaryDTO;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.exceptions.UserNotFoundException;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.auth.security.CookieFactory;
import com.njplastic.njplastic_api.auth.security.IssuedToken;
import com.njplastic.njplastic_api.auth.security.JwtTokenProvider;
import com.njplastic.njplastic_api.auth.services.AuthenticationResult;
import com.njplastic.njplastic_api.auth.services.AuthenticationService;
import com.njplastic.njplastic_api.auth.services.PasswordResetService;
import com.njplastic.njplastic_api.auth.services.UserService;
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
@Tag(name = "Authentication", description = "JWT login, refresh, logout, current user and password reset endpoints (RFC §6.2 / EP-BE-02 + EP-FE-02)")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationService authenticationService;
  private final PasswordResetService passwordResetService;
  private final UserService userService;
  private final JwtTokenProvider tokenProvider;
  private final CookieFactory cookieFactory;

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
    cookieFactory.writeAuthCookies(response, result.issued(), tokenProvider.expirationSeconds());
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
    cookieFactory.writeAuthCookies(response, issued, tokenProvider.expirationSeconds());
    return RefreshResponseDTO.builder()
        .token(issued.compact())
        .tokenType("Bearer")
        .expiresInSeconds(tokenProvider.expirationSeconds())
        .build();
  }

  @GetMapping("/me")
  @Operation(summary = "Return the authenticated user", description = "Returns the UserSummaryDTO of the principal carried by the current JWT. "
      + "Used by the Next.js frontend to rehydrate useSessionStore after a hard reload, since the "
      + "access_token cookie is httpOnly and JavaScript cannot read the user attributes from it.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Authenticated user retrieved", content = @Content(schema = @Schema(implementation = UserSummaryDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public UserSummaryDTO me(@AuthenticationPrincipal AuthenticatedUser principal) {
    User user = userService.findById(principal.id()).orElseThrow(UserNotFoundException::new);
    return UserSummaryDTO.from(user);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Clear the authentication cookies", description = "Emits Set-Cookie headers with Max-Age=0 for access_token and access_token_exp so "
      + "the browser drops the EP-FE-02 dual-cookie pair. The JWT itself stays valid until its natural "
      + "exp (the project uses single-token without server-side revocation), but the browser can no "
      + "longer present it; non-browser clients should simply discard their stored token.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Cookies cleared"),
      @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public void logout(HttpServletResponse response) {
    cookieFactory.clearAuthCookies(response);
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
}
