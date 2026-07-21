package com.koda.platform.shared.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class KodaMetricsConfigurationTest {

    private final KodaMetricsConfiguration configuration = new KodaMetricsConfiguration();

    @Test
    void deniesMetersWithTenantUserCorrelationOrSecretTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.config().meterFilter(configuration.highCardinalityTagGuardMeterFilter());

        Counter.builder("koda.business.operation")
            .tag("tenantId", "00000000-0000-4000-8000-000000000001")
            .register(registry)
            .increment();

        Counter.builder("koda.business.operation")
            .tag("userId", "user-1")
            .register(registry)
            .increment();

        Counter.builder("koda.business.operation")
            .tag("correlationId", "trace-1")
            .register(registry)
            .increment();

        Counter.builder("koda.business.operation")
            .tag("authorization", "Bearer secret")
            .register(registry)
            .increment();

        assertThat(registry.getMeters()).isEmpty();
    }

    @Test
    void allowsMetersWithStableOperationalTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.config().meterFilter(configuration.highCardinalityTagGuardMeterFilter());

        Counter.builder("koda.business.operation")
            .tag("module", "SALES")
            .tag("outcome", "SUCCESS")
            .register(registry)
            .increment();

        assertThat(registry.find("koda.business.operation").counter()).isNotNull();
    }

    @Test
    void limitsHttpServerRequestUriTagCardinality() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.config().meterFilter(KodaMetricsConfiguration.httpServerRequestsUriCardinalityMeterFilter(2));

        Counter.builder("http.server.requests").tag("uri", "/api/v1/products").register(registry).increment();
        Counter.builder("http.server.requests").tag("uri", "/api/v1/sales").register(registry).increment();
        Counter.builder("http.server.requests").tag("uri", "/api/v1/purchases").register(registry).increment();

        assertThat(registry.find("http.server.requests").tag("uri", "/api/v1/products").counter()).isNotNull();
        assertThat(registry.find("http.server.requests").tag("uri", "/api/v1/sales").counter()).isNotNull();
        assertThat(registry.find("http.server.requests").tag("uri", "/api/v1/purchases").counter()).isNull();
    }
}
