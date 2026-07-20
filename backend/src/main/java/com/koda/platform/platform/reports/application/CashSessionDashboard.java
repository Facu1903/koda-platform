package com.koda.platform.platform.reports.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashSessionDashboard(
    UUID id,
    UUID cashRegisterId,
    String cashRegisterCode,
    String cashRegisterName,
    UUID branchId,
    String status,
    BigDecimal openingAmount,
    BigDecimal expectedClosingAmount,
    String currencyCode,
    Instant openedAt,
    Instant updatedAt
) {
}