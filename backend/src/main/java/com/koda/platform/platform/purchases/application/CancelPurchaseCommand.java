package com.koda.platform.platform.purchases.application;

import java.util.UUID;

public record CancelPurchaseCommand(long version, String reason, UUID cashSessionId) {
}