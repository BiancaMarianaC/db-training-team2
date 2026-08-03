package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.ReconResultRepository;
import com.dbtraining.tradeflow.repository.TradeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReconRunIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private ReconResultRepository reconResultRepository;
    @Autowired private MeterRegistry meterRegistry;

    @Test
    void run_readsPersistedBreaksAndRecordsTimer() throws Exception {
        Trade trade = tradeRepository.save(trade("TR-I072-INT"));
        reconResultRepository.save(ReconResult.builder()
                .trade(trade)
                .discrepancyType(DiscrepancyType.PRICE_MISMATCH)
                .build());
        reconResultRepository.save(ReconResult.builder()
                .trade(trade)
                .discrepancyType(DiscrepancyType.QUANTITY_MISMATCH)
                .build());
        reconResultRepository.save(ReconResult.builder()
                .trade(trade)
                .status(ReconResult.Status.RESOLVED)
                .resolvedAt(Instant.now())
                .discrepancyType(DiscrepancyType.DATE_MISMATCH)
                .build());
        long timerBefore = meterRegistry.get("tradeflow_recon_run_seconds").timer().count();

        mockMvc.perform(post("/api/v1/recon/run")
                        .with(httpBasic("trader", "trader-pw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedCount").value(1))
                .andExpect(jsonPath("$.unmatchedCount").value(2))
                .andExpect(jsonPath("$.breakdownByType.PRICE_MISMATCH").value(1))
                .andExpect(jsonPath("$.breakdownByType.QUANTITY_MISMATCH").value(1));

        org.assertj.core.api.Assertions.assertThat(
                meterRegistry.get("tradeflow_recon_run_seconds").timer().count())
                .isEqualTo(timerBefore + 1);
    }

    private static Trade trade(String tradeRef) {
        return Trade.builder()
                .tradeRef(tradeRef)
                .instrumentId(1L)
                .counterpartyId(1L)
                .quantity(BigDecimal.ONE)
                .price(BigDecimal.TEN)
                .tradeDate(LocalDate.of(2026, 7, 31))
                .status(TradeStatus.PENDING)
                .build();
    }
}
