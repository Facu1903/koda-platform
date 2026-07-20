package com.koda.platform.platform.sales.application;

import java.util.UUID;

public record SalePaymentReversalUpdate(UUID cashSessionId, UUID cashMovementId) {
}