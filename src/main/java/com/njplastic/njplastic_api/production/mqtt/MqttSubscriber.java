package com.njplastic.njplastic_api.production.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

/**
 * Manages the MQTT connection and subscription lifecycle. Connecting is
 * resilient:
 * a broker that is unavailable at startup only logs a warning, and a periodic
 * check
 * (re)connects and (re)subscribes once it becomes reachable, so the application
 * can
 * boot without a broker in dev/CI (RFC §5.3).
 */
@Component
@RequiredArgsConstructor
public class MqttSubscriber {

  private static final Logger LOGGER = LoggerFactory.getLogger(MqttSubscriber.class);
  private static final long RECONNECT_INTERVAL_MS = 30000L;

  private final IMqttClient client;
  private final MqttConnectOptions options;
  private final MqttProperties properties;
  private final MqttListener listener;

  private volatile boolean subscribed = false;

  @PostConstruct
  void start() {
    client.setCallback(listener);
    ensureConnected();
  }

  @Scheduled(fixedDelay = RECONNECT_INTERVAL_MS)
  void ensureConnected() {
    if (client.isConnected() && subscribed) {
      return;
    }
    try {
      if (!client.isConnected()) {
        client.connect(options);
      }
      if (!subscribed) {
        client.subscribe(properties.topic(), properties.qos());
        subscribed = true;
        LOGGER.info("MQTT subscribed to topic [{}] at [{}] with QoS {}",
            properties.topic(), properties.brokerUrl(), properties.qos());
      }
    } catch (MqttException ex) {
      subscribed = false;
      if (client.isConnected()) {
        try {
          client.disconnect();
        } catch (MqttException disconnectEx) {
          LOGGER.warn("Error disconnecting MQTT client after subscribe failure: {}",
              disconnectEx.getMessage());
        }
      }
      LOGGER.warn("MQTT broker unreachable at [{}]; will retry: {}",
          properties.brokerUrl(), ex.getMessage());
    }
  }

  @PreDestroy
  void stop() {
    try {
      if (client.isConnected()) {
        client.disconnect();
      }
      client.close();
    } catch (MqttException ex) {
      LOGGER.warn("Error closing MQTT client: {}", ex.getMessage());
    } finally {
      subscribed = false;
    }
  }
}