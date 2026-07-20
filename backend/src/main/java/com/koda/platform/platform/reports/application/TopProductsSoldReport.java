package com.koda.platform.platform.reports.application;

import java.util.List;

public record TopProductsSoldReport(ReportPeriod period, List<TopProductSoldReportRow> rows) {
}