package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.config.SecurityConfig;
import com.dbtraining.tradeflow.dto.ReconResultDto;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.service.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void listResults_defaultsToOpenAndReturnsPage() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        ReconResultDto dto = new ReconResultDto(
                7L, 11L, "TRD-11", 3L, DiscrepancyType.PRICE_MISMATCH,
                ReconResult.Status.OPEN, Instant.parse("2026-07-01T10:00:00Z"), null);
        when(reconService.listBreaks(eq(ReconResult.Status.OPEN), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(dto), pageable, 1));

        mockMvc.perform(get("/api/v1/recon/results?page=0&size=10")
                        .with(httpBasic("viewer", "viewer-pw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].tradeRef").value("TRD-11"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));

        verify(reconService).listBreaks(ReconResult.Status.OPEN, null, pageable);
    }

    @Test
    void listResults_passesStatusAndCounterpartyFilters() throws Exception {
        PageRequest pageable = PageRequest.of(1, 5);
        when(reconService.listBreaks(
                ReconResult.Status.RESOLVED, 9L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get("/api/v1/recon/results")
                        .param("status", "RESOLVED")
                        .param("counterpartyId", "9")
                        .param("page", "1")
                        .param("size", "5")
                        .with(httpBasic("viewer", "viewer-pw")))
                .andExpect(status().isOk());

        verify(reconService).listBreaks(ReconResult.Status.RESOLVED, 9L, pageable);
    }

    @Test
    void listResults_withInvalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/recon/results?status=INVALID")
                        .with(httpBasic("viewer", "viewer-pw")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listResults_withoutCredentials_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/recon/results"))
                .andExpect(status().isUnauthorized());
    }
}
