package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.ReconResultRepository;
import com.dbtraining.tradeflow.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReconResultsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private ReconResultRepository reconResultRepository;

    @Test
    void results_filtersPersistedBreaksByStatusAndCounterparty() throws Exception {
        Trade matchingTrade = tradeRepository.save(trade("TR-I073-CP1", 1L));
        Trade otherTrade = tradeRepository.save(trade("TR-I073-CP2", 2L));
        reconResultRepository.save(ReconResult.builder()
                .trade(matchingTrade)
                .discrepancyType(DiscrepancyType.PRICE_MISMATCH)
                .build());
        reconResultRepository.save(ReconResult.builder()
                .trade(otherTrade)
                .discrepancyType(DiscrepancyType.QUANTITY_MISMATCH)
                .build());

        mockMvc.perform(get("/api/v1/recon/results")
                        .param("status", "OPEN")
                        .param("counterpartyId", "1")
                        .param("page", "0")
                        .param("size", "10")
                        .with(httpBasic("viewer", "viewer-pw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tradeRef").value("TR-I073-CP1"))
                .andExpect(jsonPath("$.content[0].counterpartyId").value(1))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    private static Trade trade(String tradeRef, Long counterpartyId) {
        return Trade.builder()
                .tradeRef(tradeRef)
                .instrumentId(1L)
                .counterpartyId(counterpartyId)
                .quantity(BigDecimal.ONE)
                .price(BigDecimal.TEN)
                .tradeDate(LocalDate.of(2026, 7, 31))
                .status(TradeStatus.PENDING)
                .build();
    }
}
