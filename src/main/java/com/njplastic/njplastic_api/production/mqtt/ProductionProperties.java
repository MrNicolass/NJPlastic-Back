package com.njplastic.njplastic_api.production.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Production processing tunables. Bound from "app.production.*" properties.
 * {@code timezone} drives the TIMESTAMPTZ reconstruction;
 * {@code clockToleranceMs} is the drift window that discards out-of-range
 * pulses; {@code watchdogIntervalMs} is the watchdog scan period
 * (OFFLINE); {@code autoStopMessage} is the default message applied to
 * AUTO_STOPPED records; {@code pauseScanLimit} caps how many recent
 * confirmed cycles are scanned to derive the consecutive-pause counter.
 */
@ConfigurationProperties(prefix = "app.production")
public record ProductionProperties(
        String timezone,
        long clockToleranceMs,
        long watchdogIntervalMs,
        String autoStopMessage,
        int pauseScanLimit) {
}