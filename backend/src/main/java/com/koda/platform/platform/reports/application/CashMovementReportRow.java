package com.koda.platform.platform.reports.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashMovementReportRow(
    UUID id,
    UUID cashSessionId,
    UUID cashRegisterId,
    UUID branchId,
    String movementType,
    String paymentMethod,
    BigDecimal amount,
    BigDecimal cashEffect,
    String currencyCode,
    String referenceType,
    UUID referenceId,
    UUID createdByUserId,
    Instant occurredAt
) {
}