package com.koda.platform.platform.reports.application;

import java.time.Instant;

public record ReportRange(Instant from, Instant to, int limit) {
}