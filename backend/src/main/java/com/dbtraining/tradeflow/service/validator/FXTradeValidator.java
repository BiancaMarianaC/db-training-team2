package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.exception.TradeValidationException.Code;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.FXTrade;

import java.math.BigDecimal;

public class FXTradeValidator implements ITradeValidator {

    @Override
    public void validate(BaseTrade trade) throws TradeValidationException {
        if (!(trade instanceof FXTrade ft)) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "FXTradeValidator only accepts FXTrade");
        }

        if (ft.getBaseCurrency() == null || ft.getBaseCurrency().isBlank()) {
            throw new TradeValidationException(Code.MISSING_FIELD, "baseCurrency is required");
        }
        if (ft.getQuoteCurrency() == null || ft.getQuoteCurrency().isBlank()) {
            throw new TradeValidationException(Code.MISSING_FIELD, "quoteCurrency is required");
        }
        if (ft.getSpotRate() == null) {
            throw new TradeValidationException(Code.MISSING_FIELD, "spotRate is required");
        }

        if (ft.getBaseCurrency().length() != 3 || ft.getQuoteCurrency().length() != 3) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "currencies must be ISO-4217 3-letter codes");
        }

        if (ft.getBaseCurrency().equals(ft.getQuoteCurrency())) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "base and quote currencies must differ");
        }

        if (ft.getSpotRate().signum() <= 0) {
            throw new TradeValidationException(Code.INVALID_VALUE, "spotRate must be > 0");
        }
    }
}