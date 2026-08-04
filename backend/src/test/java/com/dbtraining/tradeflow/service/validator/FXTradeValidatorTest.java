package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import com.dbtraining.tradeflow.model.FXTrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Note: FXTrade's own constructor already enforces currency-length,
 * currency-differ, and positive-spotRate invariants, so a real FXTrade
 * instance can never actually violate them — that's why every "invalid"
 * case here mocks FXTrade directly rather than building one, exercising
 * FXTradeValidator's logic in isolation from the entity's own checks.
 */
class FXTradeValidatorTest {

    private final FXTradeValidator validator = new FXTradeValidator();

    private static FXTrade validMock() {
        FXTrade trade = mock(FXTrade.class);
        when(trade.getBaseCurrency()).thenReturn("USD");
        when(trade.getQuoteCurrency()).thenReturn("EUR");
        when(trade.getSpotRate()).thenReturn(new BigDecimal("1.0850"));
        return trade;
    }

    @Test
    void validFxPasses() {
        assertThatCode(() -> validator.validate(validMock())).doesNotThrowAnyException();
    }

    @Test
    void rejectsNonFxTrade() {
        BaseTrade other = mock(EquityTrade.class);

        assertThatThrownBy(() -> validator.validate(other))
                .isInstanceOf(TradeValidationException.class)
                .extracting(e -> ((TradeValidationException) e).getCode())
                .isEqualTo(TradeValidationException.Code.INVALID_VALUE);
    }

    @Test
    void rejectsMissingBaseCurrency() {
        FXTrade trade = validMock();
        when(trade.getBaseCurrency()).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("baseCurrency is required");
    }

    @Test
    void rejectsMissingQuoteCurrency() {
        FXTrade trade = validMock();
        when(trade.getQuoteCurrency()).thenReturn(" ");

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("quoteCurrency is required");
    }

    @Test
    void rejectsMissingSpotRate() {
        FXTrade trade = validMock();
        when(trade.getSpotRate()).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("spotRate is required");
    }

    @Test
    void rejectsNonIso4217Codes() {
        FXTrade trade = validMock();
        when(trade.getBaseCurrency()).thenReturn("US");

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("ISO-4217");
    }

    @Test
    void rejectsSameBaseAndQuoteCurrency() {
        FXTrade trade = validMock();
        when(trade.getQuoteCurrency()).thenReturn("USD");

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("must differ");
    }

    @Test
    void rejectsNonPositiveSpotRate() {
        FXTrade trade = validMock();
        when(trade.getSpotRate()).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("spotRate must be > 0");
    }
}
