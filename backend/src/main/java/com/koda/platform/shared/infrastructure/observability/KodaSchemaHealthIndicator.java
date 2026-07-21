package com.koda.platform.shared.infrastructure.observability;

import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("kodaSchema")
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class KodaSchemaHealthIndicator implements HealthIndicator {

    private static final String SCHEMA_STATUS = "schema";
    private static final String CURRENT_VERSION = "currentVersion";
    private static final String PENDING_COUNT = "pendingCount";
    private static final String PENDING_VERSIONS = "pendingVersions";
    private static final String ERROR_TYPE = "errorType";

    private final Flyway flyway;

    public KodaSchemaHealthIndicator(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public Health health() {
        try {
            MigrationInfoService migrationInfo = flyway.info();
            MigrationInfo current = migrationInfo.current();
            if (current == null) {
                return Health.down()
                    .withDetail(SCHEMA_STATUS, "not_initialized")
                    .build();
            }

            MigrationInfo[] pending = migrationInfo.pending();
            if (pending.length > 0) {
                return Health.status(Status.OUT_OF_SERVICE)
                    .withDetail(SCHEMA_STATUS, "pending_migrations")
                    .withDetail(CURRENT_VERSION, versionOf(current))
                    .withDetail(PENDING_COUNT, pending.length)
                    .withDetail(PENDING_VERSIONS, pendingVersions(pending))
                    .build();
            }

            return Health.up()
                .withDetail(SCHEMA_STATUS, "current")
                .withDetail(CURRENT_VERSION, versionOf(current))
                .build();
        } catch (FlywayException exception) {
            return Health.down()
                .withDetail(SCHEMA_STATUS, "unavailable")
                .withDetail(ERROR_TYPE, exception.getClass().getSimpleName())
                .build();
        }
    }

    private List<String> pendingVersions(MigrationInfo[] pending) {
        return Arrays.stream(pending)
            .map(this::versionOf)
            .toList();
    }

    private String versionOf(MigrationInfo migrationInfo) {
        if (migrationInfo == null || migrationInfo.getVersion() == null) {
            return "repeatable";
        }
        return migrationInfo.getVersion().getVersion();
    }
}
