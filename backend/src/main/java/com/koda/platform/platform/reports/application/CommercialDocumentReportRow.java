package com.koda.platform.platform.reports.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CommercialDocumentReportRow(
    UUID id,
    UUID branchId,
    UUID partnerId,
    String partnerName,
    long documentNumber,
    String numberCode,
    String status,
    String currencyCode,
    BigDecimal totalAmount,
    String paymentStatus,
    BigDecimal paidAmount,
    Instant confirmedAt,
    Instant cancelledAt
) {
}