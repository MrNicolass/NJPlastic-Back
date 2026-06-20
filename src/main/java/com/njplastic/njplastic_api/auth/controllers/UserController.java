package com.njplastic.njplastic_api.auth.controllers;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.njplastic.njplastic_api.auth.dtos.UserRequestDTO;
import com.njplastic.njplastic_api.auth.dtos.UserResponseDTO;
import com.njplastic.njplastic_api.auth.dtos.UserUpdateRequestDTO;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.enums.UserRole;
import com.njplastic.njplastic_api.auth.exceptions.UserAlreadyExistsException;
import com.njplastic.njplastic_api.auth.exceptions.UserNotFoundException;
import com.njplastic.njplastic_api.auth.services.UserService;
import com.njplastic.njplastic_api.common.dtos.ErrorResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin endpoint for user management (sub-task 1). Backs the
 * {@code Users_Part1/2_V1} mockup screens. Every operation is MANAGER-only
 *. Soft-delete preserves the FK semantic in {@code audit_log.user_id}.
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User administration (mockup Users_Part1/2_V1)")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final PasswordEncoder passwordEncoder;

  @GetMapping
  @PreAuthorize("hasRole('MANAGER')")
  @Operation(summary = "Paginated user list", description = "Filters apply with AND semantics. All filters are optional.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Page of users", content = @Content(schema = @Schema(implementation = Page.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed to list users", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public Page<UserResponseDTO> listUsers(
      @RequestParam(required = false) UserRole role,
      @RequestParam(required = false) String sector,
      @RequestParam(required = false) String shift,
      @RequestParam(required = false) Boolean active,
      @PageableDefault(size = 50, sort = "name") Pageable pageable) {
    return userService.findPaged(role, sector, shift, active, pageable).map(UserResponseDTO::from);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('MANAGER')")
  @Operation(summary = "Get one user by UUID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown user id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public UserResponseDTO getUser(@PathVariable UUID id) {
    User user = userService.findById(id).orElseThrow(UserNotFoundException::new);
    return UserResponseDTO.from(user);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('MANAGER')")
  @Operation(summary = "Create a user", description = "Login and email must be globally unique. The password is BCrypt-hashed (factor 12) before persistence.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "User created", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "409", description = "Login or email already in use", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public UserResponseDTO createUser(@Valid @RequestBody UserRequestDTO request) {
    if (userService.existsByLogin(request.getLogin())) {
      throw new UserAlreadyExistsException("login");
    }
    if (userService.existsByEmail(request.getEmail())) {
      throw new UserAlreadyExistsException("email");
    }
    User entity = User.builder()
        .login(request.getLogin())
        .name(request.getName())
        .email(request.getEmail())
        .passwordHash(passwordEncoder.encode(request.getPassword()))
        .role(request.getRole())
        .sector(request.getSector())
        .shift(request.getShift())
        .active(true)
        .build();
    return UserResponseDTO.from(userService.create(entity));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('MANAGER')")
  @Operation(summary = "Update administrative fields of a user", description = "Login and password are immutable on this endpoint. The password rotates only through the password-reset flow.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User updated", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown user id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "409", description = "Email already in use by another user", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public UserResponseDTO updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequestDTO request) {
    User current = userService.findById(id).orElseThrow(UserNotFoundException::new);
    if (!current.getEmail().equalsIgnoreCase(request.getEmail())
        && userService.existsByEmail(request.getEmail())) {
      throw new UserAlreadyExistsException("email");
    }
    User patch = User.builder()
        .name(request.getName())
        .email(request.getEmail())
        .role(request.getRole())
        .sector(request.getSector())
        .shift(request.getShift())
        .active(request.isActive())
        .build();
    return UserResponseDTO.from(userService.update(id, patch));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('MANAGER')")
 @Operation(summary = "Soft-delete a user", description = "Flips active to false. Preserves the FK semantic in audit_log.user_id.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "User soft-deleted"),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "403", description = "Role is not allowed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
      @ApiResponse(responseCode = "404", description = "Unknown user id", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  })
  public void deleteUser(@PathVariable UUID id) {
    userService.softDelete(id);
  }
}
