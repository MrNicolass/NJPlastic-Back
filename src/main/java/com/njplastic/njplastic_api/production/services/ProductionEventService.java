package com.njplastic.njplastic_api.production.services;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.njplastic.njplastic_api.audit.entities.AuditLog;
import com.njplastic.njplastic_api.audit.services.AuditService;
import com.njplastic.njplastic_api.auth.entities.User;
import com.njplastic.njplastic_api.auth.security.AuthenticatedUser;
import com.njplastic.njplastic_api.auth.services.UserService;
import com.njplastic.njplastic_api.production.dtos.RecentEventDTO;
import com.njplastic.njplastic_api.production.entities.Machine;
import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.entities.ProductionEvent;
import com.njplastic.njplastic_api.production.enums.EventType;
import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecentEventType;
import com.njplastic.njplastic_api.production.repositories.ProductionEventRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Owns access to {@link ProductionEventRepository} (EP-BE-05 reopened).
 * Persists manual production events (training, cleaning, meetings) and exposes
 * a paginated read keyed by machine. Also aggregates the Leader "Eventos
 * recentes" feed (EP-FE-05, RFC §7.3.2 item 6) by merging manual events,
 * manual pauses, auto stops and stop-message edits into a single timeline
 * projection, scoped to the principal's accessible machines.
 */
@Service
@RequiredArgsConstructor
public class ProductionEventService {

  private static final java.util.regex.Pattern STOP_EDIT_ENDPOINT_PATTERN =
      java.util.regex.Pattern.compile("^/machines/([0-9a-fA-F-]{36})/stops/[0-9a-fA-F-]{36}/message$");

  private final ProductionEventRepository repository;
  private final MachineService machineService;
  private final MachineStatusService machineStatusService;
  private final AuditService auditService;
  private final UserService userService;
  private final ObjectMapper objectMapper;

  /**
   * Persist a new manual production event.
   *
   * @param machineId   target machine UUID (must already be access-checked by the caller)
   * @param userId      author UUID; null when no human author applies
   * @param type        event category
   * @param description free-text description
   * @param startedAt   moment the event started (UTC)
   * @param endedAt     moment the event ended; null while ongoing
   * @return the persisted entity
   */
  @Transactional
  public ProductionEvent register(UUID machineId, UUID userId, EventType type, String description,
      OffsetDateTime startedAt, OffsetDateTime endedAt) {
    ProductionEvent event = ProductionEvent.builder()
        .machineId(machineId)
        .userId(userId)
        .type(type)
        .description(description)
        .startedAt(startedAt)
        .endedAt(endedAt)
        .build();
    return repository.save(event);
  }

  /**
   * Paginated read of events for a machine, optionally constrained to a time
   * window.
   *
   * @param machineId machine UUID
   * @param from      optional inclusive start of window
   * @param to        optional exclusive end of window
   * @param pageable  paging/sort
   * @return page of events
   */
  public Page<ProductionEvent> findPaged(UUID machineId, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
    if (from != null && to != null) {
      return repository.findByMachineIdAndStartedAtBetween(machineId, from, to, pageable);
    }
    return repository.findByMachineId(machineId, pageable);
  }

  /**
   * Aggregated "Eventos recentes" feed for the Leader dashboard (EP-FE-05,
   * mockup Dashboard_Part2_V1). Merges four sources scoped to the principal's
   * accessible machines: manual events, manual pauses, auto stops and
   * stop-message edits. Each source is fetched once via its owning service;
   * the result is sorted by timestamp descending and capped at {@code limit}.
   *
   * @param principal authenticated user; drives the accessible-machine scope (RN02-RN04)
   * @param limit     maximum number of entries to return (caller validates the range)
   * @param from      inclusive lower bound on the entry timestamp
   * @param to        exclusive upper bound on the entry timestamp
   * @return merged entries ordered by timestamp descending
   */
  public List<RecentEventDTO> findRecent(AuthenticatedUser principal, int limit,
      OffsetDateTime from, OffsetDateTime to) {
    List<Machine> machines = machineService.findAccessible(principal);
    if (machines.isEmpty()) {
      return List.of();
    }
    Map<UUID, String> codeByMachineId = new HashMap<>(machines.size());
    for (Machine m : machines) {
      codeByMachineId.put(m.getId(), m.getCode());
    }
    Set<UUID> machineIds = codeByMachineId.keySet();
    Map<UUID, String> userNameCache = new HashMap<>();

    List<RecentEventDTO> merged = new ArrayList<>();
    appendManualEvents(merged, machineIds, codeByMachineId, userNameCache, from, to);
    appendStatusChanges(merged, machineIds, codeByMachineId, userNameCache, from, to);
    appendStopMessageEdits(merged, codeByMachineId, userNameCache, from, to);

    merged.sort(Comparator.comparing(RecentEventDTO::getTimestamp).reversed());
    if (merged.size() > limit) {
      return new ArrayList<>(merged.subList(0, limit));
    }
    return merged;
  }

  private void appendManualEvents(List<RecentEventDTO> sink, Set<UUID> machineIds,
      Map<UUID, String> codeByMachineId, Map<UUID, String> userNameCache,
      OffsetDateTime from, OffsetDateTime to) {
    List<ProductionEvent> events = repository
        .findByMachineIdInAndStartedAtBetweenOrderByStartedAtDesc(machineIds, from, to);
    for (ProductionEvent event : events) {
      sink.add(RecentEventDTO.builder()
          .type(RecentEventType.MANUAL_EVENT)
          .machineId(event.getMachineId())
          .machineCode(codeByMachineId.get(event.getMachineId()))
          .timestamp(event.getStartedAt())
          .description(buildEventDescription(event))
          .userId(event.getUserId())
          .userName(resolveUserName(event.getUserId(), userNameCache))
          .build());
    }
  }

  private void appendStatusChanges(List<RecentEventDTO> sink, Set<UUID> machineIds,
      Map<UUID, String> codeByMachineId, Map<UUID, String> userNameCache,
      OffsetDateTime from, OffsetDateTime to) {
    List<MachineStatus> changes = machineStatusService.findRecentStatusChanges(
        machineIds, List.of(MachineState.PAUSED, MachineState.AUTO_STOPPED), from, to);
    for (MachineStatus status : changes) {
      RecentEventType type = status.getState() == MachineState.AUTO_STOPPED
          ? RecentEventType.AUTO_STOP
          : RecentEventType.MANUAL_PAUSE;
      sink.add(RecentEventDTO.builder()
          .type(type)
          .machineId(status.getMachineId())
          .machineCode(codeByMachineId.get(status.getMachineId()))
          .timestamp(status.getStartTime())
          .description(buildStatusDescription(status))
          .userId(status.getReasonAuthorId())
          .userName(resolveUserName(status.getReasonAuthorId(), userNameCache))
          .build());
    }
  }

  private void appendStopMessageEdits(List<RecentEventDTO> sink,
      Map<UUID, String> codeByMachineId, Map<UUID, String> userNameCache,
      OffsetDateTime from, OffsetDateTime to) {
    List<AuditLog> edits = auditService.findStopMessageEditsInWindow(from, to);
    for (AuditLog log : edits) {
      UUID machineId = extractMachineIdFromStopEditEndpoint(log.getEndpoint());
      if (machineId == null || !codeByMachineId.containsKey(machineId)) {
        continue;
      }
      sink.add(RecentEventDTO.builder()
          .type(RecentEventType.STOP_MESSAGE_EDIT)
          .machineId(machineId)
          .machineCode(codeByMachineId.get(machineId))
          .timestamp(log.getTimestamp())
          .description(buildStopEditDescription(log))
          .userId(log.getUserId())
          .userName(resolveUserName(log.getUserId(), userNameCache))
          .build());
    }
  }

  private String buildEventDescription(ProductionEvent event) {
    String label = event.getType() == null ? "Evento" : event.getType().getDescription();
    if (event.getDescription() == null || event.getDescription().isBlank()) {
      return label;
    }
    return label + ": " + event.getDescription();
  }

  private String buildStatusDescription(MachineStatus status) {
    String reason = status.getReason();
    if (reason != null && !reason.isBlank()) {
      return reason;
    }
    if (status.getState() == MachineState.AUTO_STOPPED) {
      return "Parada automática detectada";
    }
    return "Pausa em andamento";
  }

  private String buildStopEditDescription(AuditLog log) {
    String payload = log.getRequestPayload();
    if (payload == null || payload.isBlank()) {
      return "Mensagem da parada atualizada";
    }
    try {
      JsonNode root = objectMapper.readTree(payload);
      JsonNode message = root.get("message");
      if (message != null && !message.isNull()) {
        String value = message.asString();
        if (value != null && !value.isBlank()) {
          return "Mensagem da parada: " + value;
        }
      }
    } catch (JacksonException ignored) {
      // Fall through to the default description below.
    }
    return "Mensagem da parada atualizada";
  }

  private UUID extractMachineIdFromStopEditEndpoint(String endpoint) {
    if (endpoint == null) {
      return null;
    }
    java.util.regex.Matcher matcher = STOP_EDIT_ENDPOINT_PATTERN.matcher(endpoint);
    if (!matcher.matches()) {
      return null;
    }
    try {
      return UUID.fromString(matcher.group(1));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private String resolveUserName(UUID userId, Map<UUID, String> cache) {
    if (userId == null) {
      return null;
    }
    String cached = cache.get(userId);
    if (cached != null) {
      return cached;
    }
    String resolved = userService.findById(userId).map(User::getName).orElse("(deleted user)");
    cache.put(userId, resolved);
    return resolved;
  }
}
