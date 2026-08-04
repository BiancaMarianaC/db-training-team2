package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.exception.InsufficientDataException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.BondTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import com.dbtraining.tradeflow.model.FXTrade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeParserTest {

    private final TradeParser parser = new TradeParser();

    @TempDir
    Path tempDir;

    private Path writeCsv(String... lines) throws IOException {
        Path file = tempDir.resolve("trades.csv");
        Files.write(file, List.of(lines));
        return file;
    }

    @Test
    void parsesEquityRow() throws IOException {
        Path file = writeCsv(
                "asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status,exchange,lot_size",
                "EQUITY,TRD-1,1,1,100,25.50,2026-03-01,PENDING,NYSE,10");

        List<BaseTrade> trades = parser.parseCsv(file);

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0)).isInstanceOf(EquityTrade.class);
        EquityTrade t = (EquityTrade) trades.get(0);
        assertThat(t.getTradeRef()).isEqualTo("TRD-1");
        assertThat(t.getExchange()).isEqualTo("NYSE");
        assertThat(t.getLotSize()).isEqualTo(10);
        assertThat(t.getStatus()).isEqualTo(TradeStatus.PENDING);
    }

    @Test
    void parsesFxRow() throws IOException {
        // Columns 0-7 are the common fields; FX-specific fields are read from
        // fixed indices 10 (baseCurrency), 11 (quoteCurrency), 12 (spotRate) —
        // build the row explicitly so the column count/positions are exact.
        String[] row = new String[13];
        row[0] = "FX"; row[1] = "TRD-2"; row[2] = "2"; row[3] = "2";
        row[4] = "1000"; row[5] = "1.1"; row[6] = "2026-03-02"; row[7] = "";
        row[8] = ""; row[9] = "";
        row[10] = "USD"; row[11] = "EUR"; row[12] = "1.0850";

        Path file = writeCsv(
                "asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status,,,base,quote,spot",
                String.join(",", row));

        List<BaseTrade> trades = parser.parseCsv(file);

        assertThat(trades).hasSize(1);
        FXTrade fx = (FXTrade) trades.get(0);
        assertThat(fx.getBaseCurrency()).isEqualTo("USD");
        assertThat(fx.getQuoteCurrency()).isEqualTo("EUR");
        assertThat(fx.getSpotRate()).isEqualByComparingTo("1.0850");
        // blank status column defaults to PENDING
        assertThat(fx.getStatus()).isEqualTo(TradeStatus.PENDING);
    }

    @Test
    void parsesBondRowWithDefaults() throws IOException {
        Path file = writeCsv(
                "asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status",
                "BOND,TRD-3,3,3,50,99.5,2026-03-03,MATCHED");

        List<BaseTrade> trades = parser.parseCsv(file);

        BondTrade bond = (BondTrade) trades.get(0);
        assertThat(bond.getCouponRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bond.getFaceValue()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(bond.getMaturityDate()).isEqualTo(bond.getTradeDate().plusYears(1));
    }

    @Test
    void skipsBlankLines() throws IOException {
        Path file = writeCsv(
                "asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status",
                "",
                "EQUITY,TRD-4,1,1,100,25.50,2026-03-01,PENDING,NYSE,10",
                "   ");

        List<BaseTrade> trades = parser.parseCsv(file);

        assertThat(trades).hasSize(1);
    }

    @Test
    void emptyFileReturnsEmptyList() throws IOException {
        Path file = tempDir.resolve("empty.csv");
        Files.createFile(file);

        assertThat(parser.parseCsv(file)).isEmpty();
    }

    @Test
    void unknownAssetClassThrowsInsufficientData() throws IOException {
        Path file = writeCsv(
                "asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status",
                "CRYPTO,TRD-5,1,1,100,25.50,2026-03-01,PENDING");

        InsufficientDataException ex = assertThrows(InsufficientDataException.class,
                () -> parser.parseCsv(file));
        assertThat(ex.getMessage()).contains("trades.csv:2").contains("unknown asset_class: CRYPTO");
    }

    @Test
    void tooFewColumnsThrowsInsufficientData() throws IOException {
        Path file = writeCsv(
                "asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status",
                "EQUITY,TRD-6,1,1,100");

        assertThrows(InsufficientDataException.class, () -> parser.parseCsv(file));
    }

    @Test
    void nonNumericFieldThrowsInsufficientData() throws IOException {
        Path file = writeCsv(
                "asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status",
                "EQUITY,TRD-7,abc,1,100,25.50,2026-03-01,PENDING");

        InsufficientDataException ex = assertThrows(InsufficientDataException.class,
                () -> parser.parseCsv(file));
        assertThat(ex.getMessage()).contains("instrument_id must be an integer");
    }

    @Test
    void missingFileThrowsUncheckedIOException() {
        Path missing = tempDir.resolve("does-not-exist.csv");
        assertThrows(UncheckedIOException.class, () -> parser.parseCsv(missing));
    }
}
