package com.njplastic.njplastic_api.production.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Production processing tunables. Bound from "app.production.*" properties.
 * {@code timezone} drives the TIMESTAMPTZ reconstruction (RN05);
 * {@code clockToleranceMs}
 * is the drift window that discards out-of-range pulses (RN05);
 * {@code watchdogIntervalMs}
 * is the watchdog scan period (OFFLINE); {@code autoStopMessage} is the default
 * message
 * applied to AUTO_STOPPED records (RF18).
 */
@ConfigurationProperties(prefix = "app.production")
public record ProductionProperties(
        String timezone,
        long clockToleranceMs,
        long watchdogIntervalMs,
        String autoStopMessage) {
}