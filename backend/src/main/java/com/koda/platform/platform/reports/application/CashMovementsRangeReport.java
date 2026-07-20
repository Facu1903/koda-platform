package com.koda.platform.platform.reports.application;

import java.util.List;

public record CashMovementsRangeReport(ReportPeriod period, CashMovementReportSummary summary, List<CashMovementReportRow> rows) {
}