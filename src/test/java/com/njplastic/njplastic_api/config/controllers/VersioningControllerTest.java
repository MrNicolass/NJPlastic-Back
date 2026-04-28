package com.njplastic.njplastic_api.config.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VersioningController.class)
class VersioningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getVersion_returns200WithVersionString() throws Exception {
        mockMvc.perform(get("/api/v1/versioning"))
                .andExpect(status().isOk())
                .andExpect(content().string("1.0.0"));
    }
}
