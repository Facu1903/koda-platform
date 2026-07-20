package com.koda.platform.platform.reports.application;

import java.math.BigDecimal;
import java.util.List;

public record LowStockReport(BigDecimal threshold, List<LowStockReportRow> rows) {
}