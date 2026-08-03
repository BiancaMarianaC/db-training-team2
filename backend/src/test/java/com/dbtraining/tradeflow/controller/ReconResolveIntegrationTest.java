package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.model.AuditLog;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.AuditLogRepository;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReconResolveIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private ReconResultRepository reconResultRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private MeterRegistry meterRegistry;

    @Test
    void resolve_updatesDatabaseWritesOneAuditAndIsIdempotent() throws Exception {
        Trade trade = tradeRepository.save(trade("TR-I074-INT"));
        ReconResult result = reconResultRepository.save(ReconResult.builder()
                .trade(trade)
                .discrepancyType(DiscrepancyType.PRICE_MISMATCH)
                .build());
        long auditBefore = auditLogRepository.count();
        double counterBefore = meterRegistry
                .get("tradeflow_recon_resolved_total").counter().count();

        mockMvc.perform(put("/api/v1/recon/{id}/resolve", result.getId())
                        .with(httpBasic("trader", "trader-pw")))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/v1/recon/{id}/resolve", result.getId())
                        .with(httpBasic("trader", "trader-pw")))
                .andExpect(status().isNoContent());

        ReconResult resolved = reconResultRepository.findById(result.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(ReconResult.Status.RESOLVED);
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(auditLogRepository.count()).isEqualTo(auditBefore + 1);
        AuditLog audit = auditLogRepository.findAll().get((int) auditBefore);
        assertThat(audit.getTableName()).isEqualTo("recon_breaks");
        assertThat(audit.getRowPk()).isEqualTo(result.getId());
        assertThat(audit.getChangedBy()).isEqualTo("trader");
        assertThat(meterRegistry.get("tradeflow_recon_resolved_total").counter().count())
                .isEqualTo(counterBefore + 1);
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
