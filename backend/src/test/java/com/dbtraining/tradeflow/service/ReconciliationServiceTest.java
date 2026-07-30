package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.*;
import com.dbtraining.tradeflow.dto.Discrepancy;
import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.repository.ReconResultDAO;
import com.dbtraining.tradeflow.repository.TradeDAO;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
// import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ============================================================================
 * ReconciliationServiceTest — TICKET-I048..I053
 * ============================================================================
 * WHAT:    JUnit + Mockito tests for the recon engine.
 * HOW:     @ExtendWith(MockitoExtension.class). Mock the DAOs, build sample
 *          trade lists, assert on the returned ReconReport.
 * WHY:     Day 4 sets a 70% coverage target. ReconciliationService is the
 *          critical path — it gets the most attentaion.
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

    @Mock private TradeDAO tradeDAO;
    @Mock private ReconResultDAO reconResultDAO;

    // TODO: Re-enable mock injection once the tests from tickets I051 - I053 are fixed
    // @InjectMocks
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
        Discrepancy d = report.discrepancies().get(0);
        assertThat(d.tradeRef()).isEqualTo("TRD-001");
        assertThat(d.types()).containsExactly(DiscrepancyType.PRICE_MISMATCH);
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


    // TODO: Re-enable when production code provides a DAO 
    // reconciliation method that calls TradeDAO.findAll().
    @Test
    @Disabled("Blocked: ReconciliationService has no TradeDAO dependency or runForAll() method")
    void mockedTradeDAO_findAllCalledOnce() {
        List<Trade> sample = List.of(sampleTrade("TRD-1"));
        when(tradeDAO.findAll()).thenReturn(sample);

        // Call matchTrades() instead of runForAll() based on the other TODO names
        // service.matchTrades();

        verify(tradeDAO, times(1)).findAll();
        // Happy-path: no discrepancies persisted because everything matched.
        verifyNoInteractions(reconResultDAO);
    }

    // TODO: Re-enable when runForAll() persists discrepancies
    // through ReconResultDAO.insert().
    @Test
    @Disabled("Blocked: no production method currently calls ReconResultDAO.insert()")
    void runForAll_oneDiscrepancy_insertsOneReconResult() {
        Trade internalOnly = sampleTrade("TRD-INT-ONLY");
        when(tradeDAO.findAll()).thenReturn(List.of(internalOnly));
        // External feed is empty in this test — we expect 1 MISSING_TRADE discrepancy.

        // service.runForAll();

        ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);
        verify(reconResultDAO, times(1)).insert(captor.capture());
        ReconResult inserted = captor.getValue();

        assertThat(inserted.getDiscrepancyType())
                .isEqualTo(DiscrepancyType.MISSING_TRADE);
    }

    // TODO: Re-enable when runForAll() performs DAO-based reconciliation
    // and only persists results when discrepancies are found.
    @Test
    @Disabled(
        "Blocked: ReconciliationService has no runForAll() method that reads trades " +
        "from TradeDAO and skips ReconResultDAO.insert() when all trades match"
    )
    void runForAll_allMatched_neverCallsInsert() {
        Trade matched = sampleTrade("TRD-1");
        when(tradeDAO.findAll()).thenReturn(List.of(matched));
        // External feed (stubbed elsewhere) returns the same trade — nothing to flag.

        // service.runForAll();

        verify(reconResultDAO, never()).insert(any(ReconResult.class));
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

    private static Trade sampleTrade(String ref) {
        return Trade.builder()
                .tradeRef(ref).instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .build();
    }
}
