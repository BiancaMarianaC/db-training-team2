package com.dbtraining.tradeflow.exception;

/**
 * ============================================================================
 * InsufficientDataException — TICKET-I033
 * ============================================================================
 * WHAT:    Unchecked exception for when a required field is null/missing AND
 *          the caller has no way to recover.
 * HOW:     extends RuntimeException.
 * WHY:     Distinguishes "the caller made a mistake" (unchecked) from
 *          "an upstream check failed but is recoverable" (TradeValidationException).
 * OBSERVE: Thrown by Builder.build() when a required field is null.
 * VERIFIED (TICKET-I033): extends RuntimeException (unchecked); both the
 * (message) and (message, cause) constructors are present, matching the
 * acceptance criteria.
 * ============================================================================
 */
public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String message) {
        super(message);
    }

    public InsufficientDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
