package com.koda.platform.platform.cash.application;

import java.math.BigDecimal;

public record CloseCashSessionCommand(long version, BigDecimal countedClosingAmount) {
}