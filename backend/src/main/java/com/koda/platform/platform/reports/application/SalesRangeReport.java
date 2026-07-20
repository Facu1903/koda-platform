package com.koda.platform.platform.reports.application;

import java.util.List;

public record SalesRangeReport(ReportPeriod period, CommercialReportSummary summary, List<CommercialDocumentReportRow> rows) {
}