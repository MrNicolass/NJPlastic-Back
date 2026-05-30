package com.njplastic.njplastic_api.production.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQTT client settings. Bound from "app.mqtt.*" properties. The backend
 * subscribes to a single topic shared by every machine (RFC §5.3); credentials
 * are optional and left blank when the broker allows anonymous access.
 */
@ConfigurationProperties(prefix = "app.mqtt")
public record MqttProperties(
    String brokerUrl,
    String clientId,
    String topic,
    int qos,
    String username,
    String password,
    boolean cleanSession,
    boolean automaticReconnect,
    int connectionTimeout,
    int keepAliveInterval) {
}