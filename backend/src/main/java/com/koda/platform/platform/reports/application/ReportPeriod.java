package com.koda.platform.platform.reports.application;

import java.time.Instant;

public record ReportPeriod(Instant from, Instant to) {
}