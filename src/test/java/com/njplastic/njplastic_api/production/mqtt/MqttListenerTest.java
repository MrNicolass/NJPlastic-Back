package com.njplastic.njplastic_api.production.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.njplastic.njplastic_api.production.services.ProductionService;

@ExtendWith(MockitoExtension.class)
class MqttListenerTest {

  @Mock
  private ProductionService productionService;

  private MqttListener listener;

  @BeforeEach
  void setUp() {
    listener = new MqttListener(new ObjectMapper(), productionService);
  }

  private MqttMessage message(String body) {
    MqttMessage m = new MqttMessage(body.getBytes(StandardCharsets.UTF_8));
    m.setQos(1);
    return m;
  }

  @Test
  void messageArrived_delegatesValidPayloadToProductionService() {
    MqttMessage message = message("{\"machine_code\":\"MAQ-01\",\"generated_at\":\"14:23:55\"}");

    listener.messageArrived("njplastic/pulso", message);

    ArgumentCaptor<PulsePayload> captor = ArgumentCaptor.forClass(PulsePayload.class);
    verify(productionService).processPulse(captor.capture(), any(OffsetDateTime.class));
    PulsePayload captured = captor.getValue();
    assertThat(captured.getMachineCode()).isEqualTo("MAQ-01");
    assertThat(captured.getGeneratedAt()).isEqualTo("14:23:55");
  }

  @Test
  void messageArrived_dropsMalformedJson() {
    MqttMessage message = message("not-json");

    assertThatCode(() -> listener.messageArrived("njplastic/pulso", message)).doesNotThrowAnyException();

    verifyNoInteractions(productionService);
  }

  @Test
  void messageArrived_dropsPayloadWithMissingMachineCode() {
    MqttMessage message = message("{\"generated_at\":\"14:23:55\"}");

    listener.messageArrived("njplastic/pulso", message);

    verify(productionService, never()).processPulse(any(), any());
  }

  @Test
  void messageArrived_dropsPayloadWithMissingGeneratedAt() {
    MqttMessage message = message("{\"machine_code\":\"MAQ-01\"}");

    listener.messageArrived("njplastic/pulso", message);

    verify(productionService, never()).processPulse(any(), any());
  }

  @Test
  void connectionLost_handlesNullCause() {
    assertThatCode(() -> listener.connectionLost(null)).doesNotThrowAnyException();
  }

  @Test
  void connectionLost_handlesCauseWithMessage() {
    assertThatCode(() -> listener.connectionLost(new RuntimeException("network down"))).doesNotThrowAnyException();
  }

  @Test
  void deliveryComplete_isNoOp() {
    assertThatCode(() -> listener.deliveryComplete(null)).doesNotThrowAnyException();
    verifyNoInteractions(productionService);
  }

  @Test
  void messageArrived_acceptsAnyTopic() {
    MqttMessage message = message("{\"machine_code\":\"MAQ-99\",\"generated_at\":\"00:00:00\"}");
    listener.messageArrived("some/other/topic", message);
    verify(productionService).processPulse(any(PulsePayload.class), any(OffsetDateTime.class));
  }
}
