package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconReportExporterTest {

    private final ReconReportExporter exporter = new ReconReportExporter();

    @TempDir
    Path tempDir;

    private static Trade tradeWithRef(String tradeRef) {
        return Trade.builder()
                .tradeRef(tradeRef).instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("25.50"))
                .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.PENDING)
                .build();
    }

    @Test
    void writesHeaderAndOneRowPerResult() throws IOException {
        ReconResult open = ReconResult.builder()
                .trade(tradeWithRef("TRD-1"))
                .discrepancyType(DiscrepancyType.PRICE_MISMATCH)
                .status(ReconResult.Status.OPEN)
                .build();

        Path target = tempDir.resolve("report.csv");
        exporter.exportReconReport(List.of(open), target);

        List<String> lines = Files.readAllLines(target);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).isEqualTo("trade_ref,status,discrepancy_type,resolved_at");
        assertThat(lines.get(1)).isEqualTo("TRD-1,OPEN,PRICE_MISMATCH,");
    }

    @Test
    void resolvedResultIncludesResolvedAt() throws IOException {
        ReconResult resolved = ReconResult.builder()
                .trade(tradeWithRef("TRD-2"))
                .discrepancyType(DiscrepancyType.QUANTITY_MISMATCH)
                .status(ReconResult.Status.OPEN)
                .build();
        resolved.resolve();

        Path target = tempDir.resolve("report.csv");
        exporter.exportReconReport(List.of(resolved), target);

        List<String> lines = Files.readAllLines(target);
        assertThat(lines.get(1)).startsWith("TRD-2,RESOLVED,QUANTITY_MISMATCH,");
        assertThat(lines.get(1)).doesNotEndWith(",");
    }

    @Test
    void emptyResultsWritesOnlyHeader() throws IOException {
        Path target = tempDir.resolve("empty.csv");

        exporter.exportReconReport(List.of(), target);

        assertThat(Files.readAllLines(target)).containsExactly(
                "trade_ref,status,discrepancy_type,resolved_at");
    }

    @Test
    void fieldsContainingCommasAreQuoted() throws IOException {
        ReconResult withComma = ReconResult.builder()
                .trade(tradeWithRef("TRD,3"))
                .discrepancyType(DiscrepancyType.DATE_MISMATCH)
                .status(ReconResult.Status.OPEN)
                .build();

        Path target = tempDir.resolve("report.csv");
        exporter.exportReconReport(List.of(withComma), target);

        List<String> lines = Files.readAllLines(target);
        assertThat(lines.get(1)).startsWith("\"TRD,3\",OPEN,DATE_MISMATCH");
    }

    @Test
    void overwritesExistingFileAtomically() throws IOException {
        Path target = tempDir.resolve("report.csv");
        Files.writeString(target, "stale content\n");

        ReconResult r = ReconResult.builder()
                .trade(tradeWithRef("TRD-4"))
                .discrepancyType(DiscrepancyType.MISSING_TRADE)
                .status(ReconResult.Status.OPEN)
                .build();
        exporter.exportReconReport(List.of(r), target);

        List<String> lines = Files.readAllLines(target);
        assertThat(lines).doesNotContain("stale content");
        assertThat(lines).hasSize(2);
    }

    @Test
    void createsParentDirectoriesIfMissing() throws IOException {
        Path target = tempDir.resolve("nested/dir/report.csv");

        exporter.exportReconReport(List.of(), target);

        assertThat(Files.exists(target)).isTrue();
    }
}
