package com.njplastic.njplastic_api.erp.repositories.rows;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Minimal projection of a local production_cycle that is about to be written to
 * the ERP via the configurable {@code app.datasource.erp.query.insert-cycle}
 * statement. The local UUID is the idempotency key in the ERP - retries after
 * a failed write must not duplicate apontamentos (RN08). Not an API contract.
 *
 * @param id             local production_cycle UUID, used as idempotency key
 * @param machineCode    machine code the cycle belongs to
 * @param pulseTimestamp reconstructed pulse timestamp (RN05)
 * @param sequence       monotonic sequence per machine
 * @param intervalMs     milliseconds since the previous confirmed cycle (RF07)
 */
public record ProductionCycleRow(
    UUID id,
    String machineCode,
    OffsetDateTime pulseTimestamp,
    Long sequence,
    Integer intervalMs) {
}