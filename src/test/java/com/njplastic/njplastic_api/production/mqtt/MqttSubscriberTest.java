package com.njplastic.njplastic_api.production.mqtt;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MqttSubscriberTest {

  @Mock
  private IMqttClient client;

  @Mock
  private MqttConnectOptions options;

  @Mock
  private MqttListener listener;

  private MqttSubscriber subscriber;

  private MqttProperties properties() {
    return new MqttProperties(
        "tcp://localhost:1883",
        "njplastic-backend",
        "njplastic/pulso",
        1,
        "",
        "",
        true,
        true,
        10,
        60);
  }

  @BeforeEach
  void setUp() {
    subscriber = new MqttSubscriber(client, options, properties(), listener);
  }

  @Test
  void start_setsCallbackAndAttemptsConnection() throws MqttException {
    when(client.isConnected()).thenReturn(false);

    subscriber.start();

    verify(client).setCallback(listener);
    verify(client).connect(options);
    verify(client).subscribe("njplastic/pulso", 1);
  }

  @Test
  void ensureConnected_skipsWhenAlreadyConnected() throws MqttException {
    when(client.isConnected()).thenReturn(true);

    subscriber.ensureConnected();

    verify(client, never()).connect(options);
    verify(client, never()).subscribe(anyString(), anyInt());
  }

  @Test
  void ensureConnected_swallowsBrokerFailure() throws MqttException {
    when(client.isConnected()).thenReturn(false);
    doThrow(new MqttException(MqttException.REASON_CODE_BROKER_UNAVAILABLE)).when(client).connect(options);

    assertThatCode(() -> subscriber.ensureConnected()).doesNotThrowAnyException();

    verify(client, times(1)).connect(options);
    verify(client, never()).subscribe(anyString(), anyInt());
  }

  @Test
  void stop_disconnectsAndClosesWhenConnected() throws MqttException {
    when(client.isConnected()).thenReturn(true);

    subscriber.stop();

    verify(client).disconnect();
    verify(client).close();
  }

  @Test
  void stop_doesNotDisconnectWhenDisconnected() throws MqttException {
    when(client.isConnected()).thenReturn(false);

    subscriber.stop();

    verify(client, never()).disconnect();
    verify(client).close();
  }

  @Test
  void stop_swallowsCloseFailure() throws MqttException {
    when(client.isConnected()).thenReturn(true);
    doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION)).when(client).close();

    assertThatCode(() -> subscriber.stop()).doesNotThrowAnyException();
  }
}
