package com.njplastic.njplastic_api.production.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class PulsePayloadTest {

  @Test
  void builder_populatesFields() {
    PulsePayload payload = PulsePayload.builder()
        .machineCode("MAQ-01")
        .generatedAt("14:23:55")
        .build();
    assertThat(payload.getMachineCode()).isEqualTo("MAQ-01");
    assertThat(payload.getGeneratedAt()).isEqualTo("14:23:55");
  }

  @Test
  void toString_includesBothFields() {
    PulsePayload payload = PulsePayload.builder()
        .machineCode("MAQ-01")
        .generatedAt("14:23:55")
        .build();
    String text = payload.toString();
    assertThat(text)
        .contains("PulsePayload{")
        .contains("machineCode=MAQ-01")
        .contains("generatedAt=14:23:55");
  }

  @Test
  void jacksonDeserializesSnakeCaseFields() throws Exception {
    String json = "{\"machine_code\":\"MAQ-01\",\"generated_at\":\"14:23:55\"}";
    PulsePayload payload = new ObjectMapper().readValue(json, PulsePayload.class);
    assertThat(payload.getMachineCode()).isEqualTo("MAQ-01");
    assertThat(payload.getGeneratedAt()).isEqualTo("14:23:55");
  }
}
