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

    @Test
    void matchTrades_priceMismatch_flagsDiscrepancy() {
        BaseTrade in  = equityWith("TRD-001", new BigDecimal("100"),
                                new BigDecimal("245.50"), LocalDate.of(2026, 3, 1));
        BaseTrade out = equityWith("TRD-001", new BigDecimal("100"),
                                new BigDecimal("249.99"), LocalDate.of(2026, 3, 1));

        ReconReport report = service.matchTrades(List.of(in), List.of(out));

        assertThat(report.matched()).isEmpty();
        assertThat(report.discrepancies()).hasSize(1);
        var discrepancy = report.discrepancies().get(0);
        assertThat(discrepancy.tradeRef()).isEqualTo("TRD-001");
        assertThat(discrepancy.types()).containsExactly(DiscrepancyType.PRICE_MISMATCH);
    }

    /** Scale-difference regression test: 245.5 vs 245.50 are equal by compareTo(). */
    @Test
    void matchTrades_priceScaleDifference_notFlagged() {
        BaseTrade in  = equityWith("TRD-002", new BigDecimal("100"),
                                new BigDecimal("245.5"),  LocalDate.of(2026, 3, 1));
        BaseTrade out = equityWith("TRD-002", new BigDecimal("100"),
                                new BigDecimal("245.50"), LocalDate.of(2026, 3, 1));

        ReconReport report = service.matchTrades(List.of(in), List.of(out));

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.matched()).hasSize(1);
    }

    @Test
    void matchTrades_missingExternal_flagsMissingTrade() {
        List<BaseTrade> internal = List.of(equity("TRD-INT-ONLY"));
        List<BaseTrade> external = List.of();

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).hasSize(1);
        assertThat(report.discrepancies().get(0).tradeRef()).isEqualTo("TRD-INT-ONLY");
        assertThat(report.discrepancies().get(0).types())
                .containsExactly(DiscrepancyType.MISSING_TRADE);
    }

    @Test
    void matchTrades_missingInternal_flagsMissingTrade() {
        List<BaseTrade> internal = List.of();
        List<BaseTrade> external = List.of(equity("TRD-EXT-ONLY"));

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).hasSize(1);
        assertThat(report.discrepancies().get(0).tradeRef()).isEqualTo("TRD-EXT-ONLY");
        assertThat(report.discrepancies().get(0).types())
                .containsExactly(DiscrepancyType.MISSING_TRADE);
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

    private static BaseTrade equityWith(String tradeRef, BigDecimal qty, BigDecimal price, LocalDate date) {
        return EquityTrade.builder()
                .tradeRef(tradeRef).instrumentId(1L).counterpartyId(1L)
                .quantity(qty).price(price).tradeDate(date)
                .status(TradeStatus.MATCHED).exchange("XETRA").lotSize(100)
                .build();
    }
}


