package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
// TODO(TICKET-I079): Re-add these imports when ReconciliationService
// accepts ReconResultRepository and MeterRegistry constructor dependencies.
// import com.dbtraining.tradeflow.repository.ReconResultRepository;
// import io.micrometer.core.instrument.MeterRegistry;
// import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
// import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * ============================================================================
 * ReconciliationServiceTest — TICKET-I048..I053
 * ============================================================================
 * WHAT:    JUnit + Mockito tests for the recon engine.
 * HOW:     @ExtendWith(MockitoExtension.class). Mock the DAOs, build sample
 *          trade lists, assert on the returned ReconReport.
 * WHY:     Day 4 sets a 70% coverage target. ReconciliationService is the
 *          critical path — it gets the most attention.
 * OBSERVE: `mvn test` runs these in a few seconds; JaCoCo report shows the
 *          coverage % per class.
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {
    /** TODO(TICKET-I079): Re-enable when ReconciliationService accepts 
        reconResultRepository and MeterRegistry */
    // @Mock private ReconResultRepository reconResultRepository;
    // private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private ReconciliationService service;

    // TODO(TICKET-I079): Update this to pass reconResultRepository and
    // meterRegistry once those constructor dependencies are added.
    // i.e. service = new ReconciliationService(reconResultRepository, meterRegistry);
    @BeforeEach
    void setUp() {
        service = new ReconciliationService();
    }

    @Test
    void matchTrades_allMatched_returnsEmptyDiscrepancies() {
        List<BaseTrade> internal = List.of(equity("TRD-001"), equity("TRD-002"), equity("TRD-003"));
        List<BaseTrade> external = List.of(equity("TRD-001"), equity("TRD-002"), equity("TRD-003"));

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.matched()).hasSize(3);
        assertThat(report.totalInternal()).isEqualTo(3);
        assertThat(report.totalExternal()).isEqualTo(3);
    }

    // TODO(TICKET-I049): test matchTrades_priceMismatch_flagsDiscrepancy.
    @Test
    void matchTrades_priceMismatch_flagsDiscrepancy() {
        fail("TICKET-I049: implement test");
    }

    // TODO(TICKET-I050): test matchTrades_missingExternal_flagsMissingTrade.
    @Test
    void matchTrades_missingExternal_flagsMissingTrade() {
        fail("TICKET-I050: implement test");
    }

    // TODO(TICKET-I051): test with @Mock TradeDAO + verify(...).findAll() called.
    @Test
    void mockedTradeDAO_findAllCalledOnce() {
        fail("TICKET-I051: implement test");
    }

    // TODO(TICKET-I052): test with @Mock ReconResultDAO + ArgumentCaptor.
    @Test
    void mockedReconResultDAO_insertCalledPerDiscrepancy() {
        fail("TICKET-I052: implement test");
    }

    private static BaseTrade equity(String tradeRef) {
        return EquityTrade.builder()
                .tradeRef(tradeRef).instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETRA").lotSize(100)
                .build();
    }
}


