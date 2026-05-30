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

@Repository
public interface MachineStatusRepository extends JpaRepository<MachineStatus, UUID> {

  Optional<MachineStatus> findTopByMachineIdAndEndTimeIsNullOrderByStartTimeDesc(UUID machineId);

  Optional<MachineStatus> findTopByMachineIdOrderByStartTimeDesc(UUID machineId);

  /**
   * Status records of the given states that overlap the window [from, to]. A
   * record overlaps when it starts before the window ends and is either still
   * open
   * or ends after the window starts. Used to compute downtime for OEE (RF10).
   *
   * @param machineId the machine UUID
   * @param states    the operational states to include (downtime states)
   * @param from      window start
   * @param to        window end
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
}