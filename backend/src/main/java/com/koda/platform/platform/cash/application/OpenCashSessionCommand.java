package com.koda.platform.platform.cash.application;

import java.math.BigDecimal;
import java.util.UUID;

public record OpenCashSessionCommand(UUID cashRegisterId, BigDecimal openingAmount, String currencyCode) {
}