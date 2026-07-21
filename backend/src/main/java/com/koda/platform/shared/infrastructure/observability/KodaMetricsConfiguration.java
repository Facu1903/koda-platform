package com.koda.platform.shared.infrastructure.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class KodaMetricsConfiguration {

    static final int MAX_HTTP_SERVER_REQUEST_URI_TAGS = 100;

    private static final Set<String> BLOCKED_HIGH_CARDINALITY_TAGS = Set.of(
        "authorization",
        "correlationId",
        "correlation_id",
        "email",
        "jwt",
        "requestId",
        "request_id",
        "sessionId",
        "session_id",
        "tenantId",
        "tenant_id",
        "token",
        "userId",
        "user_id"
    );

    @Bean
    MeterFilter highCardinalityTagGuardMeterFilter() {
        return new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                return denyHighCardinalityTags(id);
            }
        };
    }

    @Bean
    MeterFilter httpServerRequestsUriCardinalityMeterFilter() {
        return httpServerRequestsUriCardinalityMeterFilter(MAX_HTTP_SERVER_REQUEST_URI_TAGS);
    }

    static MeterFilter httpServerRequestsUriCardinalityMeterFilter(int maxUriTags) {
        return MeterFilter.maximumAllowableTags("http.server.requests", "uri", maxUriTags, MeterFilter.deny());
    }

    private static MeterFilterReply denyHighCardinalityTags(Meter.Id id) {
        boolean hasBlockedTag = id.getTags().stream()
            .map(tag -> tag.getKey())
            .anyMatch(BLOCKED_HIGH_CARDINALITY_TAGS::contains);

        return hasBlockedTag ? MeterFilterReply.DENY : MeterFilterReply.NEUTRAL;
    }
}
