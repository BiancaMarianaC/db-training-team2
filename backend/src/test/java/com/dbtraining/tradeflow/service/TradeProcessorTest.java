package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.Discrepancy;
import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeProcessorTest {

    @Mock private TradeParser parser;
    @Mock private TradeValidator validator;
    @Mock private ReconciliationService recon;
    @Mock private ReconReportExporter exporter;

    private TradeProcessor processor;

    private final Path internalCsv = Path.of("internal.csv");
    private final Path externalCsv = Path.of("external.csv");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new TradeProcessor(parser, validator, recon, exporter);
    }

    @Test
    void processParsesValidatesAndReconciles() {
        List<BaseTrade> internal = List.of();
        List<BaseTrade> external = List.of();
        when(parser.parseCsv(internalCsv)).thenReturn(internal);
        when(parser.parseCsv(externalCsv)).thenReturn(external);
        when(validator.validateAll(internal)).thenReturn(Collections.emptyList());
        when(validator.validateAll(external)).thenReturn(Collections.emptyList());

        ReconReport report = new ReconReport(0, 0, List.of(), List.of());
        when(recon.matchTrades(internal, external)).thenReturn(report);

        ReconSummary summary = new ReconSummary(0, 0, 0, 0, Collections.emptyMap());
        when(recon.generateReport(report)).thenReturn(summary);
        when(recon.render(summary)).thenReturn("Reconciliation summary\n");

        ReconSummary result = processor.process(internalCsv, externalCsv);

        assertThat(result).isSameAs(summary);
        verify(parser).parseCsv(internalCsv);
        verify(parser).parseCsv(externalCsv);
        verify(recon).matchTrades(internal, external);
        verify(recon).generateReport(report);
    }

    @Test
    void validationFindingsAreLoggedButDoNotAbortProcessing() throws Exception {
        List<BaseTrade> internal = List.of();
        List<BaseTrade> external = List.of();
        when(parser.parseCsv(internalCsv)).thenReturn(internal);
        when(parser.parseCsv(externalCsv)).thenReturn(external);

        TradeValidationException finding = new TradeValidationException(
                TradeValidationException.Code.MISSING_FIELD, "exchange is required");
        when(validator.validateAll(internal)).thenReturn(List.of(finding));
        when(validator.validateAll(external)).thenReturn(Collections.emptyList());

        ReconReport report = new ReconReport(0, 0, List.of(),
                List.of(new Discrepancy("TRD-1", List.of(DiscrepancyType.PRICE_MISMATCH))));
        when(recon.matchTrades(internal, external)).thenReturn(report);

        ReconSummary summary = new ReconSummary(1, 1, 0, 1, Collections.emptyMap());
        when(recon.generateReport(report)).thenReturn(summary);
        when(recon.render(any())).thenReturn("summary");

        ReconSummary result = processor.process(internalCsv, externalCsv);

        assertThat(result.unmatchedCount()).isEqualTo(1);
        verify(recon).matchTrades(internal, external);
    }
}
