package com.njplastic.njplastic_api.production.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.Test;

class MqttConfigTest {

  private final MqttConfig config = new MqttConfig();

  private MqttProperties propertiesWith(String username, String password) {
    return new MqttProperties(
        "tcp://localhost:1883",
        "njplastic-backend",
        "njplastic/pulso",
        1,
        username,
        password,
        true,
        true,
        10,
        60);
  }

  @Test
  void mqttConnectOptions_appliesBaseSettings() {
    MqttConnectOptions options = config.mqttConnectOptions(propertiesWith("", ""));

    assertThat(options.isCleanSession()).isTrue();
    assertThat(options.isAutomaticReconnect()).isTrue();
    assertThat(options.getConnectionTimeout()).isEqualTo(10);
    assertThat(options.getKeepAliveInterval()).isEqualTo(60);
    assertThat(options.getUserName()).isNull();
  }

  @Test
  void mqttConnectOptions_setsCredentialsWhenUsernamePresent() {
    MqttConnectOptions options = config.mqttConnectOptions(propertiesWith("user", "pass"));

    assertThat(options.getUserName()).isEqualTo("user");
    assertThat(options.getPassword()).containsExactly('p', 'a', 's', 's');
  }

  @Test
  void mqttConnectOptions_emptyPasswordWhenNull() {
    MqttConnectOptions options = config.mqttConnectOptions(propertiesWith("user", null));

    assertThat(options.getUserName()).isEqualTo("user");
    assertThat(options.getPassword()).isEmpty();
  }

  @Test
  void mqttConnectOptions_skipsCredentialsWhenUsernameBlank() {
    MqttConnectOptions options = config.mqttConnectOptions(propertiesWith("   ", "pass"));

    assertThat(options.getUserName()).isNull();
  }

  @Test
  void mqttClient_buildsDisconnectedClient() throws MqttException {
    IMqttClient client = config.mqttClient(propertiesWith("", ""));

    assertThat(client).isNotNull();
    assertThat(client.isConnected()).isFalse();
    assertThat(client.getClientId()).isEqualTo("njplastic-backend");
    client.close();
  }
}
