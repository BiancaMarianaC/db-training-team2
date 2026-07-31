package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.config.SecurityConfig;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.service.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReconController.class)
@Import(SecurityConfig.class)
class ReconControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ReconciliationService reconService;

    @Test
    void run_asTrader_returnsSummary() throws Exception {
        when(reconService.runForAll()).thenReturn(
                new ReconSummary(5, 5, 3, 2, Map.of()));

        mockMvc.perform(post("/api/v1/recon/run")
                        .with(httpBasic("trader", "trader-pw")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.matchedCount").value(3))
                .andExpect(jsonPath("$.unmatchedCount").value(2));

        verify(reconService).runForAll();
    }

    @Test
    void run_asAdmin_isAllowed() throws Exception {
        when(reconService.runForAll()).thenReturn(
                new ReconSummary(0, 0, 0, 0, Map.of()));

        mockMvc.perform(post("/api/v1/recon/run")
                        .with(httpBasic("admin", "admin-pw")))
                .andExpect(status().isOk());
    }

    @Test
    void run_asViewer_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/recon/run")
                        .with(httpBasic("viewer", "viewer-pw")))
                .andExpect(status().isForbidden());
    }

    @Test
    void run_withoutCredentials_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/recon/run"))
                .andExpect(status().isUnauthorized());
    }
}
