package com.njplastic.njplastic_api.production.repositories;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.njplastic.njplastic_api.production.entities.MachineStatus;
import com.njplastic.njplastic_api.production.enums.MachineState;
import com.njplastic.njplastic_api.production.enums.RecordState;
import org.springframework.data.domain.Pageable;

@Repository
public interface MachineStatusRepository extends JpaRepository<MachineStatus, UUID> {

	Optional<MachineStatus> findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(UUID machineId);

	Optional<MachineStatus> findTopByMachineIdOrderByStartTimeDesc(UUID machineId);

	Optional<MachineStatus> findTopByMachineIdAndStateAndReasonIsNullOrderByStartTimeDesc(
			UUID machineId, MachineState state);

	/**
 * Most recent closed status record of a machine in the given state whose
 * {@code endTime} matches the supplied instant. Sole purpose is the merge of
 * contiguous PAUSED segments in {@code MachineStatusService}: when a new gap
 * starts exactly where the previous one ended, the previous record is
 * extended instead of producing a duplicate row.
 *
 * @param machineId the machine UUID
 * @param state the operational state to match
 * @param endTime the exact {@code endTime} to match
 * @return the contiguous-tail record, if any
 */
	Optional<MachineStatus> findTopByMachineIdAndStateAndEndTimeOrderByStartTimeDesc(
			UUID machineId, MachineState state, OffsetDateTime endTime);

	/**
 * Status records of a machine whose window overlaps {@code [from, to]},
 * regardless of state. A record overlaps when it starts before the window
 * ends and is either still open or ends after the window starts. Used to
 * build the status timeline and the shift report.
 *
 * @param machineId the machine UUID
 * @param from window start
 * @param to window end
 * @return overlapping records ordered by start time
 */
	@Query("""
			SELECT ms FROM MachineStatus ms
			WHERE ms.machineId = :machineId
			  AND ms.startTime < :to
			  AND (ms.endTime IS NULL OR ms.endTime > :from)
			ORDER BY ms.startTime ASC
			""")
	List<MachineStatus> findWindow(
			@Param("machineId") UUID machineId,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to);

	/**
 * Status records of a machine in the given states whose window overlaps
 * {@code [from, to]}. Used by the shift report to list manual
 * pauses and auto stops separately.
 *
 * @param machineId the machine UUID
 * @param states the operational states to include
 * @param from window start
 * @param to window end
 * @return overlapping records ordered by start time
 */
	@Query("""
			SELECT ms FROM MachineStatus ms
			WHERE ms.machineId = :machineId
			  AND ms.state IN :states
			  AND ms.startTime < :to
			  AND (ms.endTime IS NULL OR ms.endTime > :from)
			ORDER BY ms.startTime ASC
			""")
	List<MachineStatus> findWindowByStates(
			@Param("machineId") UUID machineId,
			@Param("states") Collection<MachineState> states,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to);

	/**
 * Status records of the given states that overlap the window [from, to]. A
 * record overlaps when it starts before the window ends and is either still
 * open
 * or ends after the window starts. Used to compute downtime for OEE.
 *
 * @param machineId the machine UUID
 * @param states the operational states to include (downtime states)
 * @param from window start
 * @param to window end
 * @return overlapping status records
 */
	@Query("""
			SELECT ms FROM MachineStatus ms
			WHERE ms.machineId = :machineId
			  AND ms.state IN :states
			  AND ms.startTime < :to
			  AND (ms.endTime IS NULL OR ms.endTime > :from)
			""")
	List<MachineStatus> findOverlapping(
			@Param("machineId") UUID machineId,
			@Param("states") Collection<MachineState> states,
			@Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to);

	List<MachineStatus> findByRecordStateAndStateInOrderByStartTimeAsc(
			RecordState recordState, Collection<MachineState> states, Pageable pageable);

	/**
 * Status records whose {@code startTime} falls in the window, scoped to a
 * set of machines and a set of states. Backs the Leader "Eventos recentes"
 * feed (mockup Dashboard_Part2_V1), where pauses and auto stops
 * appear when they begin and not while they remain ongoing.
 *
 * @param machineIds accessible machines for the principal 
 * @param states PAUSED and/or AUTO_STOPPED for the recent-events feed
 * @param from inclusive lower bound on startTime
 * @param to exclusive upper bound on startTime
 * @return matching records ordered by startTime descending
 */
	List<MachineStatus> findByMachineIdInAndStateInAndStartTimeBetweenOrderByStartTimeDesc(
			Collection<UUID> machineIds, Collection<MachineState> states,
			OffsetDateTime from, OffsetDateTime to);
}