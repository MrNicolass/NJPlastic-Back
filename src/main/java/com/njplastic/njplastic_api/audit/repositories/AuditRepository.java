package com.njplastic.njplastic_api.audit.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.audit.entities.AuditLog;

/**
 * Append-only access to the "audit_log" table. Only the
 * inherited insert path and read-side queries are used; no update or delete
 * operation is declared, preserving the immutability of the audit trail.
 * {@link JpaSpecificationExecutor} powers the paginated/filtered admin read
 * exposed by {@code GET /audit-logs}.
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

 /**
 * Successful entries for a given HTTP method whose endpoint matches the
 * supplied prefix, ordered by capture time descending. Backs the edition
 * history of an AUTO_STOPPED message reconstructed from
 * {@code audit_log} without a dedicated table.
 *
 * @param httpMethod HTTP verb of the captured request
 * @param endpointPrefix path prefix matched with {@code LIKE prefix%}
 * @param httpStatus HTTP status code that qualifies as a successful
 * edition (typically 200)
 * @param pageable paging/sort
 * @return page of audit entries that match every filter
 */
  Page<AuditLog> findByHttpMethodAndEndpointStartingWithAndHttpStatusOrderByTimestampDesc(
      String httpMethod, String endpointPrefix, int httpStatus, Pageable pageable);

 /**
 * Latest successful entry strictly before {@code before} for a given
 * HTTP method and endpoint prefix. Used to resolve the
 * {@code previousMessage} of the first row of a page, since the row
 * preceding the page is not part of the page itself.
 *
 * @param httpMethod HTTP verb of the captured request
 * @param endpointPrefix path prefix matched with {@code LIKE prefix%}
 * @param httpStatus HTTP status code that qualifies as a successful
 * edition (typically 200)
 * @param before exclusive upper bound on {@code timestamp}
 * @return the latest matching entry, or empty when none exists
 */
  Optional<AuditLog> findTopByHttpMethodAndEndpointStartingWithAndHttpStatusAndTimestampBeforeOrderByTimestampDesc(
      String httpMethod, String endpointPrefix, int httpStatus, OffsetDateTime before);

 /**
 * Successful {@code PUT /machines/{id}/stops/{id}/message} edits captured in
 * the given window, ordered by capture time descending. Backs the Leader
 * "Eventos recentes" feed (item 6): each
 * matching row materializes a {@code STOP_MESSAGE_EDIT} entry on the
 * dashboard timeline. The endpoint is anchored at both ends via a JPQL LIKE
 * with two wildcards so paths from the rest of the API are excluded.
 *
 * @param from inclusive lower bound on timestamp
 * @param to exclusive upper bound on timestamp
 * @return matching audit entries ordered by timestamp descending
 */
  @Query("""
      SELECT al FROM AuditLog al
      WHERE al.httpMethod = 'PUT'
        AND al.httpStatus = 200
        AND al.endpoint LIKE '/machines/%/stops/%/message'
        AND al.timestamp >= :from
        AND al.timestamp < :to
      ORDER BY al.timestamp DESC
      """)
  List<AuditLog> findStopMessageEditsInWindow(
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to);
}
