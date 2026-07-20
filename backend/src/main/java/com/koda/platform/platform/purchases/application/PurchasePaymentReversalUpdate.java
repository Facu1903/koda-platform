package com.koda.platform.platform.purchases.application;

import java.util.UUID;

public record PurchasePaymentReversalUpdate(UUID cashSessionId, UUID cashMovementId) {
}