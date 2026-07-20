package com.koda.platform.platform.sales.application;

import java.util.UUID;

public record ConfirmSaleCommand(long version, UUID cashSessionId, String paymentMethod) {
}