package com.koda.platform.platform.purchases.application;

import java.util.UUID;

public record ConfirmPurchaseCommand(long version, UUID cashSessionId, String paymentMethod) {
}