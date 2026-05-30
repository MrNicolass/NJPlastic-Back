package com.njplastic.njplastic_api.production.mqtt;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njplastic.njplastic_api.production.services.ProductionService;

import lombok.RequiredArgsConstructor;

/**
 * Inbound MQTT adapter for the shared pulse topic (RFC §5.3). Deserializes each
 * message into a {@link PulsePayload} and delegates to
 * {@link ProductionService};
 * malformed payloads are logged and dropped so a single bad message cannot
 * stall
 * the subscription. All business rules live in the service - this adapter
 * carries
 * none (RFC §5.3).
 */
@Component
@RequiredArgsConstructor
public class MqttListener implements MqttCallback {

  private static final Logger LOGGER = LoggerFactory.getLogger(MqttListener.class);

  private final ObjectMapper objectMapper;
  private final ProductionService productionService;

  @Override
  public void connectionLost(Throwable cause) {
    LOGGER.warn("MQTT connection lost; automatic reconnect will retry: {}",
        cause == null ? "unknown cause" : cause.getMessage());
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) {
    OffsetDateTime receivedAt = OffsetDateTime.now();
    String body = new String(message.getPayload(), StandardCharsets.UTF_8);
    PulsePayload payload;
    try {
      payload = objectMapper.readValue(body, PulsePayload.class);
    } catch (Exception ex) {
      LOGGER.warn("Discarding malformed MQTT pulse on topic [{}]: {}", topic, ex.getMessage());
      return;
    }
    if (payload.getMachineCode() == null || payload.getGeneratedAt() == null) {
      LOGGER.warn("Discarding MQTT pulse with missing fields on topic [{}]: {}", topic, payload);
      return;
    }
    productionService.processPulse(payload, receivedAt);
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
    // No-op: the backend only subscribes, it does not publish.
  }
}