package com.koda.platform.platform.purchases.application;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchasePaymentUpdate(UUID cashSessionId, String paymentMethod, BigDecimal paidAmount, UUID cashMovementId) {
}