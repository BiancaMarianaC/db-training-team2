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

class EquityTradeValidatorTest {

    private final EquityTradeValidator validator = new EquityTradeValidator();

    @Test
    void validEquityPasses() {
        EquityTrade trade = mock(EquityTrade.class);
        when(trade.getExchange()).thenReturn("NYSE");
        when(trade.getLotSize()).thenReturn(10);
        when(trade.getQuantity()).thenReturn(new BigDecimal("100"));

        assertThatCode(() -> validator.validate(trade)).doesNotThrowAnyException();
    }

    @Test
    void rejectsNonEquityTrade() {
        BaseTrade other = mock(FXTrade.class);

        assertThatThrownBy(() -> validator.validate(other))
                .isInstanceOf(TradeValidationException.class)
                .extracting(e -> ((TradeValidationException) e).getCode())
                .isEqualTo(TradeValidationException.Code.INVALID_VALUE);
    }

    @Test
    void rejectsBlankExchange() {
        EquityTrade trade = mock(EquityTrade.class);
        when(trade.getExchange()).thenReturn("  ");

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("exchange is required");
    }

    @Test
    void rejectsNonPositiveLotSize() {
        EquityTrade trade = mock(EquityTrade.class);
        when(trade.getExchange()).thenReturn("NYSE");
        when(trade.getLotSize()).thenReturn(0);

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("lotSize must be > 0");
    }

    @Test
    void rejectsQuantityNotAMultipleOfLotSize() {
        EquityTrade trade = mock(EquityTrade.class);
        when(trade.getExchange()).thenReturn("NYSE");
        when(trade.getLotSize()).thenReturn(10);
        when(trade.getQuantity()).thenReturn(new BigDecimal("101"));

        assertThatThrownBy(() -> validator.validate(trade))
                .isInstanceOf(TradeValidationException.class)
                .hasMessageContaining("whole multiple of lotSize");
    }
}
