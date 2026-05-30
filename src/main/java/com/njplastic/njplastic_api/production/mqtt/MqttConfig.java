package com.njplastic.njplastic_api.production.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Eclipse Paho client wiring for the production pulse subscription and the
 * scheduling support used by the OFFLINE watchdog. Builds the connect options
 * and
 * a disconnected {@link IMqttClient} from {@link MqttProperties}; the
 * connection
 * and subscription lifecycle is managed by {@link MqttSubscriber} so a broker
 * that
 * is down at startup does not crash the application (RFC §5.3).
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({ MqttProperties.class, ProductionProperties.class })
public class MqttConfig {

  @Bean
  MqttConnectOptions mqttConnectOptions(MqttProperties properties) {
    MqttConnectOptions options = new MqttConnectOptions();
    options.setCleanSession(properties.cleanSession());
    options.setAutomaticReconnect(properties.automaticReconnect());
    options.setConnectionTimeout(properties.connectionTimeout());
    options.setKeepAliveInterval(properties.keepAliveInterval());
    if (properties.username() != null && !properties.username().isBlank()) {
      options.setUserName(properties.username());
      options.setPassword(properties.password() == null ? new char[0] : properties.password().toCharArray());
    }
    return options;
  }

  @Bean
  IMqttClient mqttClient(MqttProperties properties) throws MqttException {
    return new MqttClient(properties.brokerUrl(), properties.clientId(), new MemoryPersistence());
  }
}