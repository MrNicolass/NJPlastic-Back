package com.njplastic.njplastic_api.production.dtos;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Single entry of the edition history of an AUTO_STOPPED message (*). Reconstructed from the append-only {@code audit_log}
 * table, so no new persistence is needed: every edition flowed through
 * {@code PUT /machines/{id}/stops/{stopId}/message} and was captured by
 * the global {@code AuditFilter}. Used by {@code GET
 * /machines/{id}/stops/{stopId}/edits} to back the "Histórico de edições"
 * block of the Modal_Change_Stop mockups (Líder/Gestor variants).
 */
@Schema(description = "Edition entry of an AUTO_STOPPED message, derived from audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StopEditDTO {

  @Schema(description = "Moment the edition was persisted", example = "2026-05-28T14:42:11Z", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private OffsetDateTime editedAt;

  @Schema(description = "Author UUID; null when the request was anonymous (should not happen on this protected route)", example = "3f1c2b9e-7a4d-4e2a-9b8c-1d2e3f4a5b6c", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private UUID authorId;

  @Schema(description = "Human name of the author resolved from the users aggregate; placeholder when the account was soft-deleted", example = "Maria Souza", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String authorName;

  @Schema(description = "Message stored before this edition; null when this is the first known edition of the stop", example = "Stop detected automatically after 3 consecutive pauses", requiredMode = NOT_REQUIRED, accessMode = READ_ONLY, nullable = true)
  private String previousMessage;

  @Schema(description = "Message stored by this edition", example = "Mold change - batch 4321", requiredMode = REQUIRED, accessMode = READ_ONLY, nullable = false)
  private String newMessage;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("StopEditDTO{editedAt=").append(editedAt)
        .append(", authorId=").append(authorId)
        .append(", authorName=").append(authorName)
        .append(", previousMessage=").append(previousMessage)
        .append(", newMessage=").append(newMessage)
        .append('}');
    return sb.toString();
  }
}
