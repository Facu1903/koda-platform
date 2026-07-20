package com.koda.platform.platform.cash.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCashMovementCommand(
    UUID cashSessionId,
    String movementType,
    String paymentMethod,
    BigDecimal amount,
    String currencyCode,
    String referenceType,
    UUID referenceId,
    String description
) {
}