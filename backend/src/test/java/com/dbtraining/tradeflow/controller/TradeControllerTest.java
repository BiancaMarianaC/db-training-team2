package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.config.SecurityConfig;
import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.exception.GlobalExceptionHandler;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ============================================================================
 * TradeControllerTest — TICKET-I082
 * ============================================================================
 * WHAT:    @WebMvcTest slice for POST /api/v1/trades happy path.
 * WHY:     TICKET-I069 only proved this manually with curl. This test pins
 *          the 201 + Location contract Day 7's frontend integration depends
 *          on, so a future change can't silently break it.
 * ============================================================================
 */
@WebMvcTest(TradeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TradeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TradeService tradeService;

    @Test
    void createTrade_validInput_returns201() throws Exception {
        TradeDto saved = new TradeDto(
                42L, "TRD-2026-0099", 1L, 1L,
                new BigDecimal("100"), new BigDecimal("250.50"),
                LocalDate.of(2026, 3, 1), TradeStatus.PENDING, Instant.parse("2026-07-31T00:00:00Z"));
        when(tradeService.createTrade(any())).thenReturn(saved);

        String requestJson = """
                {
                    "tradeRef": "TRD-2026-0099",
                    "instrumentId": 1,
                    "counterpartyId": 1,
                    "quantity": 100,
                    "price": 250.50,
                    "tradeDate": "2026-03-01"
                }
                """;

        mockMvc.perform(post("/api/v1/trades")
                        .with(httpBasic("trader", "trader-pw"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/trades/42"))
                .andExpect(jsonPath("$.tradeRef").value("TRD-2026-0099"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * TICKET-I083: missing quantity fails @NotNull on TradeRequest.quantity,
     * caught by GlobalExceptionHandler's MethodArgumentNotValidException
     * handler -> code "VALIDATION_FAILED" + details.quantity.
     */
    @Test
    void createTrade_missingQuantity_returns400() throws Exception {
        String requestJson = """
                {
                    "tradeRef": "TRD-2026-0099",
                    "instrumentId": 1,
                    "counterpartyId": 1,
                    "price": 250.50,
                    "tradeDate": "2026-03-01"
                }
                """;

        mockMvc.perform(post("/api/v1/trades")
                        .with(httpBasic("trader", "trader-pw"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.quantity").exists());
    }

    /**
     * TICKET-I083: negative quantity fails @Positive on TradeRequest.quantity,
     * same error envelope as the missing-field case above.
     */
    @Test
    void createTrade_negativeQuantity_returns400() throws Exception {
        String requestJson = """
                {
                    "tradeRef": "TRD-2026-0099",
                    "instrumentId": 1,
                    "counterpartyId": 1,
                    "quantity": -100,
                    "price": 250.50,
                    "tradeDate": "2026-03-01"
                }
                """;

        mockMvc.perform(post("/api/v1/trades")
                        .with(httpBasic("trader", "trader-pw"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.quantity").exists());
    }
}
