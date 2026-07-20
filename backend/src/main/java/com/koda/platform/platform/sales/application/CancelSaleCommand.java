package com.koda.platform.platform.sales.application;

import java.util.UUID;

public record CancelSaleCommand(long version, String reason, UUID cashSessionId) {
}