package com.koda.platform.platform.licensing.application;

import java.time.Instant;

public record UpdateTenantProductEntitlementCommand(long version, String status, Instant validUntil) {
}
