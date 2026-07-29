package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.*;
import com.dbtraining.tradeflow.repository.ReconResultDAO;
import com.dbtraining.tradeflow.repository.TradeDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
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

    @Mock private TradeDAO tradeDAO;
    @Mock private ReconResultDAO reconResultDAO;

    @InjectMocks
    private ReconciliationService service;

    // TODO(TICKET-I048): test matchTrades_allMatched_returnsEmptyDiscrepancies.
    @Test
    void matchTrades_allMatched_returnsEmptyDiscrepancies() {
        fail("TICKET-I048: implement test");
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
        List<Trade> sample = List.of(sampleTrade("TRD-1"));
        when(tradeDAO.findAll()).thenReturn(sample);

        // Call matchTrades() instead of runForAll() based on the other TODO names
        // service.matchTrades();

        verify(tradeDAO, times(1)).findAll();
        // Happy-path: no discrepancies persisted because everything matched.
        verifyNoInteractions(reconResultDAO);
    }

    // TODO(TICKET-I052): test with @Mock ReconResultDAO + ArgumentCaptor.
    @Test
    void mockedReconResultDAO_insertCalledPerDiscrepancy() {
        fail("TICKET-I052: implement test");
    }

    private static Trade sampleTrade(String ref) {
        return Trade.builder()
                .tradeRef(ref)
                .quantity(new BigDecimal("100")).price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .build();
    }
}