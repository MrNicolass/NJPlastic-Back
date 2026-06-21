package com.njplastic.njplastic_api.production.services;

import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.njplastic.njplastic_api.production.mqtt.ProductionProperties;

import lombok.RequiredArgsConstructor;

/**
 * Resolves the active shift label from the current wall-clock time. The
 * window boundaries follow the seed conventions used across the codebase:
 * TURNO_A from 06:00 to 14:00, TURNO_B from 14:00 to 22:00 and TURNO_C from
 * 22:00 to 06:00 of the next day. The configured production timezone is
 * applied so the result stays correct regardless of the JVM default zone.
 */
@Component
@RequiredArgsConstructor
public class ShiftResolver {

  public static final String SHIFT_A = "TURNO_A";
  public static final String SHIFT_B = "TURNO_B";
  public static final String SHIFT_C = "TURNO_C";

  private static final LocalTime SHIFT_A_START = LocalTime.of(6, 0);
  private static final LocalTime SHIFT_B_START = LocalTime.of(14, 0);
  private static final LocalTime SHIFT_C_START = LocalTime.of(22, 0);

  private final ProductionProperties properties;

 /**
 * Resolve the active shift label using the configured production timezone.
 *
 * @return one of TURNO_A, TURNO_B, TURNO_C
 */
  public String currentShift() {
    ZoneId zone = ZoneId.of(properties.timezone());
    LocalTime now = LocalTime.now(zone);
    if (now.isBefore(SHIFT_A_START)) {
      return SHIFT_C;
    }
    if (now.isBefore(SHIFT_B_START)) {
      return SHIFT_A;
    }
    if (now.isBefore(SHIFT_C_START)) {
      return SHIFT_B;
    }
    return SHIFT_C;
  }
}
