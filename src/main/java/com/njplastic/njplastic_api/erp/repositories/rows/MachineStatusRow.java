package com.njplastic.njplastic_api.erp.repositories.rows;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Minimal projection of a local machine_status (PAUSED / AUTO_STOPPED) record
 * about to be written to the ERP via the configurable
 * {@code app.datasource.erp.query.insert-pause} statement. The local UUID is
 * the idempotency key in the ERP - retries after a failed write must not
 * duplicate downtime records (RN08). Not an API contract.
 *
 * @param id               local machine_status UUID, used as idempotency key
 * @param machineCode      machine code the record belongs to
 * @param state            PAUSED or AUTO_STOPPED (string to keep the row
 *                         vendor-free)
 * @param reason           classification reason chosen by the operator (RF09)
 * @param message          editable message of an auto-stop record (RF18, RF19)
 * @param startTime        transition start timestamp
 * @param endTime          transition end timestamp (null when still active)
 * @param consecutiveCount snapshot of the consecutive-pause counter (RN09)
 */
public record MachineStatusRow(
    UUID id,
    String machineCode,
    String state,
    String reason,
    String message,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    Integer consecutiveCount) {
}