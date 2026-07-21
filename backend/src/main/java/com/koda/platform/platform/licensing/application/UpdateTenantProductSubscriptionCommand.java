package com.koda.platform.platform.licensing.application;

import java.time.Instant;

public record UpdateTenantProductSubscriptionCommand(long version, String status, Instant validUntil) {
}
