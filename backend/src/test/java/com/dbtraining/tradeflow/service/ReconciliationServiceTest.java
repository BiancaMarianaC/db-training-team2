package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.*;
import com.dbtraining.tradeflow.dto.Discrepancy;
import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.repository.ReconResultRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
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
    @Mock private ReconResultRepository reconResultRepository;
    private SimpleMeterRegistry meterRegistry;
    /** ReconResultDAO was removed as part of TICKET-I061
        as it was no longer used at that point */
    // @Mock private ReconResultDAO reconResultDAO;

    // TODO: Re-enable mock injection once the tests from tickets I051 - I053 are fixed
    // @InjectMocks
    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new ReconciliationService(reconResultRepository, meterRegistry);
    }

    @Test
    void runForAll_returnsSummaryAndRecordsMetric() {
        ReconResult priceBreak = ReconResult.builder()
                .trade(sampleTrade("TRD-PRICE"))
                .discrepancyType(DiscrepancyType.PRICE_MISMATCH)
                .build();
        ReconResult quantityBreak = ReconResult.builder()
                .trade(sampleTrade("TRD-QTY"))
                .discrepancyType(DiscrepancyType.QUANTITY_MISMATCH)
                .build();
        when(reconResultRepository.countByStatus(ReconResult.Status.RESOLVED)).thenReturn(3L);
        when(reconResultRepository.countByStatus(ReconResult.Status.OPEN)).thenReturn(2L);
        when(reconResultRepository.findByStatus(ReconResult.Status.OPEN))
                .thenReturn(List.of(priceBreak, quantityBreak));

        var summary = service.runForAll();

        assertThat(summary.totalInternal()).isEqualTo(5);
        assertThat(summary.totalExternal()).isEqualTo(5);
        assertThat(summary.matchedCount()).isEqualTo(3);
        assertThat(summary.unmatchedCount()).isEqualTo(2);
        assertThat(summary.breakdownByType())
                .containsEntry(DiscrepancyType.PRICE_MISMATCH, 1)
                .containsEntry(DiscrepancyType.QUANTITY_MISMATCH, 1)
                .containsEntry(DiscrepancyType.DATE_MISMATCH, 0)
                .containsEntry(DiscrepancyType.MISSING_TRADE, 0);
        assertThat(meterRegistry.get("tradeflow_recon_run_seconds").timer().count())
                .isEqualTo(1);
    }

    @Test
    void listBreaks_filtersByStatusAndCounterpartyAndMapsDto() {
        Trade trade = sampleTrade("TRD-OPEN");
        ReconResult result = ReconResult.builder()
                .trade(trade)
                .discrepancyType(DiscrepancyType.PRICE_MISMATCH)
                .build();
        PageRequest pageable = PageRequest.of(0, 10);
        when(reconResultRepository.findByStatusAndCounterpartyId(
                ReconResult.Status.OPEN, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(result), pageable, 1));

        var page = service.listBreaks(ReconResult.Status.OPEN, 1L, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).tradeRef()).isEqualTo("TRD-OPEN");
        assertThat(page.getContent().get(0).counterpartyId()).isEqualTo(1L);
        assertThat(page.getContent().get(0).discrepancyType())
                .isEqualTo(DiscrepancyType.PRICE_MISMATCH);
        verify(reconResultRepository).findByStatusAndCounterpartyId(
                ReconResult.Status.OPEN, 1L, pageable);
    }

    @Test
    void listBreaks_withoutCounterpartyUsesStatusQuery() {
        PageRequest pageable = PageRequest.of(1, 5);
        when(reconResultRepository.findByStatus(ReconResult.Status.RESOLVED, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.listBreaks(ReconResult.Status.RESOLVED, null, pageable);

        verify(reconResultRepository).findByStatus(ReconResult.Status.RESOLVED, pageable);
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


    // TODO: Re-enable if production code provides a DAO 
    // reconciliation method that calls TradeDAO.findAll().
    /*
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
    } */

    // TODO: Re-enable if runForAll() persists discrepancies
    // through ReconResultDAO.insert().
    /*
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
    } */

    // TODO: Re-enable if runForAll() performs DAO-based reconciliation
    // and only persists results when discrepancies are found.
    /*
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
    } */

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
