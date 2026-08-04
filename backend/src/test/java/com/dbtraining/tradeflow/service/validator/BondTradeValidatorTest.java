package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.BondTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Note: BondTrade's own constructor already enforces couponRate range,
 * maturityDate-after-tradeDate, and positive-faceValue invariants, so a real
 * BondTrade instance can never actually violate them — every "invalid" case
 * here mocks BondTrade directly to exercise BondTradeValidator's logic in
 * isolation from the entity's own checks.
 */
class BondTradeValidatorTest {

    private final BondTradeValidator validator = new BondTradeValidator();

    private static BondTrade validMock() {
        BondTrade trade = mock(BondTrade.class);
        when(trade.getTradeDate()).thenReturn(LocalDate.of(2026, 3, 1));
        when(trade.getCouponRate()).thenReturn(new BigDecimal("4.5"));
        when(trade.getMaturityDate()).thenReturn(LocalDate.of(2030, 3, 1));
        when(trade.getFaceValue()).thenReturn(new BigDecimal("1000"));
        return trade;
    }

    @Test
    void validBondPasses() {
        assertThatCode(() -> validator.validate(validMock())).doesNotThrowAnyException();
    }

    @Test
    void rejectsNonBondTrade() {
        BaseTrade other = mock(EquityTrade.class);

        assertThatThrownBy(() -> validator.validate(other))
                .isInstanceOf(TradeValidationException.class)
                .extracting(e -> ((TradeValidationException) e).getCode())
                .isEqualTo(TradeValidationException.Code.INVALID_VALUE);
    }

    @Test
    void rejectsMissingCouponRate() {
        BondTrade trade = validMock();
        when(trade.getCouponRate()).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("couponRate is required");
    }

    @Test
    void rejectsMissingMaturityDate() {
        BondTrade trade = validMock();
        when(trade.getMaturityDate()).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("maturityDate is required");
    }

    @Test
    void rejectsMissingFaceValue() {
        BondTrade trade = validMock();
        when(trade.getFaceValue()).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("faceValue is required");
    }

    @Test
    void rejectsCouponRateOutOfRange() {
        BondTrade trade = validMock();
        when(trade.getCouponRate()).thenReturn(new BigDecimal("150"));

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("couponRate must be between 0 and 100");
    }

    @Test
    void rejectsMaturityBeforeTradeDate() {
        BondTrade trade = validMock();
        when(trade.getMaturityDate()).thenReturn(LocalDate.of(2020, 1, 1));

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("maturityDate must be after tradeDate");
    }

    @Test
    void rejectsNonPositiveFaceValue() {
        BondTrade trade = validMock();
        when(trade.getFaceValue()).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("faceValue must be > 0");
    }
}
