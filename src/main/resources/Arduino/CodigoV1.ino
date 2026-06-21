#include "SoftwareSerial.h"
#include <PubSubClient.h>
#include "WiFiEspAT.h"
#include <NTPClient.h>
#include <WiFiUdp.h>

SoftwareSerial ESP_Serial(10, 11);
WiFiClient espClient;
WiFiUDP ntpUDP;
PubSubClient mqttClient(espClient);
NTPClient timeClient(ntpUDP, "pool.ntp.org", -10800, 60000);

//Wifi
char ssid[] = "";
char pass[] = "";
IPAddress ip(192, 168, 1, 100);

//MQTT
const char* mqttServer = "";
const int mqttPort = 1883;
const char* sendTopic = "njplastic/pulso";
const char* machineCode = "";

//Data
int inPin = 4;
int actualState = 0;
int previousState = 0;

void reconnect_mqtt() {
  while (!mqttClient.connected()) {
    if (mqttClient.connect("ArduinoClient")) {
      Serial.println("Connected!");
    } else {
      Serial.print("Failed, code: ");
      Serial.println(mqttClient.state());
      delay(5000);
    }
  }
}

void reconnect_wifi() {
  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("WiFi shield not present");
    while (true);
  }

  WiFi.config(ip);

  while (WiFi.status() != WL_CONNECTED) {
    WiFi.begin(ssid, pass);
    delay(1000);
  }
}

void setup(){
  Serial.begin(9600);
  ESP_Serial.begin(9600);

  pinMode(inPin, INPUT_PULLUP);

  WiFi.init(&ESP_Serial);

  mqttClient.setServer(mqttServer, mqttPort);

  reconnect_wifi();
  reconnect_mqtt();
  timeClient.begin();
}

void loop(){
  if (WiFi.status() == WL_CONNECTED){
    
    if (!mqttClient.connected()) {
      reconnect_mqtt();
    }
    mqttClient.loop();
    timeClient.update();

    actualState = !digitalRead(inPin);

    if (actualState != previousState) {
      if (actualState == 1) {
        mqttClient.publish(sendTopic, String("{\"machine_code\":\"" + machineCode + "\",\"generated_at\":\"" + timeClient.getFormattedTime() + "\"}").c_str());
      }
      delay(50);
    }

    previousState = actualState;
  } else {
    reconnect_wifi();
  }
}