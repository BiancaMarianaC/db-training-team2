package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.exception.TradeValidationException.Code;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.BondTrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BondTradeValidator implements ITradeValidator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public void validate(BaseTrade trade) throws TradeValidationException {
        if (!(trade instanceof BondTrade bt)) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "BondTradeValidator only accepts BondTrade");
        }

        if (bt.getCouponRate() == null) {
            throw new TradeValidationException(Code.MISSING_FIELD, "couponRate is required");
        }
        if (bt.getMaturityDate() == null) {
            throw new TradeValidationException(Code.MISSING_FIELD, "maturityDate is required");
        }
        if (bt.getFaceValue() == null) {
            throw new TradeValidationException(Code.MISSING_FIELD, "faceValue is required");
        }

        if (bt.getCouponRate().signum() < 0 || bt.getCouponRate().compareTo(HUNDRED) > 0) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "couponRate must be between 0 and 100");
        }

        if (!bt.getMaturityDate().isAfter(bt.getTradeDate())) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "maturityDate must be after tradeDate");
        }

        if (bt.getFaceValue().signum() <= 0) {
            throw new TradeValidationException(Code.INVALID_VALUE, "faceValue must be > 0");
        }
    }
}