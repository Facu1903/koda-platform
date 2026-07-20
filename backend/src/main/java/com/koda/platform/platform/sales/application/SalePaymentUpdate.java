package com.koda.platform.platform.sales.application;

import java.math.BigDecimal;
import java.util.UUID;

public record SalePaymentUpdate(UUID cashSessionId, String paymentMethod, BigDecimal paidAmount, UUID cashMovementId) {
}