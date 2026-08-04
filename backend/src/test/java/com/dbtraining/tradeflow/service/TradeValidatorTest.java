package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.BondTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import com.dbtraining.tradeflow.model.FXTrade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.service.validator.BondTradeValidator;
import com.dbtraining.tradeflow.service.validator.EquityTradeValidator;
import com.dbtraining.tradeflow.service.validator.FXTradeValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TradeValidatorTest {

    private final TradeValidator validator = new TradeValidator(
            new EquityTradeValidator(), new FXTradeValidator(), new BondTradeValidator());

    private static EquityTrade validEquity() {
        return EquityTrade.builder()
                .tradeRef("EQ-1").instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("25.50"))
                .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.PENDING)
                .exchange("NYSE").lotSize(10)
                .build();
    }

    private static FXTrade validFx() {
        return FXTrade.builder()
                .tradeRef("FX-1").instrumentId(2L).counterpartyId(2L)
                .quantity(new BigDecimal("1000")).price(new BigDecimal("1.1"))
                .tradeDate(LocalDate.of(2026, 3, 2)).status(TradeStatus.PENDING)
                .baseCurrency("USD").quoteCurrency("EUR").spotRate(new BigDecimal("1.0850"))
                .build();
    }

    private static BondTrade validBond() {
        return BondTrade.builder()
                .tradeRef("BOND-1").instrumentId(3L).counterpartyId(3L)
                .quantity(new BigDecimal("50")).price(new BigDecimal("99.5"))
                .tradeDate(LocalDate.of(2026, 3, 3)).status(TradeStatus.PENDING)
                .couponRate(new BigDecimal("4.5")).maturityDate(LocalDate.of(2030, 3, 3))
                .faceValue(new BigDecimal("1000"))
                .build();
    }

    @Test
    void validBatchAcrossAllAssetClassesProducesNoFindings() {
        List<BaseTrade> trades = List.of(validEquity(), validFx(), validBond());

        assertThat(validator.validateAll(trades)).isEmpty();
    }

    @Test
    void invalidEquityLotSizeIsReported() {
        EquityTrade badEquity = EquityTrade.builder()
                .tradeRef("EQ-2").instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("25.50"))
                .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.PENDING)
                .exchange("NYSE").lotSize(3) // 100 is not a multiple of 3
                .build();

        List<TradeValidationException> findings = validator.validateAll(List.of(badEquity));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getCode()).isEqualTo(TradeValidationException.Code.INVALID_VALUE);
        assertThat(findings.get(0).getMessage()).contains("lotSize");
    }

    // NOTE: FXTrade/BondTrade constructors already enforce currency-length,
    // currency-differ, and maturity-after-trade-date invariants themselves
    // (throwing IllegalStateException at construction) — so those specific
    // ITradeValidator branches are unreachable via normal Builder use and
    // aren't exercised here. EquityTrade's constructor doesn't duplicate the
    // lotSize/quantity check, so that branch is the one worth covering below.

    @Test
    void multipleFindingsAreCollectedNotFailFast() {
        EquityTrade badEquity = EquityTrade.builder()
                .tradeRef("EQ-3").instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("101")).price(new BigDecimal("25.50"))
                .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.PENDING)
                .exchange("NYSE").lotSize(10) // 101 is not a whole multiple of 10
                .build();

        List<TradeValidationException> findings =
                validator.validateAll(List.of(badEquity, validFx(), validBond()));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getMessage()).contains("lotSize");
    }

    @Test
    void unregisteredAssetClassProducesInvalidValueFinding() {
        BaseTrade noValidator = new BaseTrade("TRD-X", 1L, 1L,
                BigDecimal.TEN, BigDecimal.ONE, LocalDate.of(2026, 3, 1),
                TradeStatus.PENDING, null) {
            @Override
            public String assetClassDescription() {
                return "UNREGISTERED";
            }
        };

        List<TradeValidationException> findings = validator.validateAll(List.of(noValidator));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getCode()).isEqualTo(TradeValidationException.Code.INVALID_VALUE);
        assertThat(findings.get(0).getMessage()).contains("no validator registered");
    }
}
