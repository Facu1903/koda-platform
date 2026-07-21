package com.koda.platform.shared.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class KodaSchemaHealthIndicatorTest {

    @Test
    void reportsUpWhenSchemaIsCurrent() {
        MigrationInfo current = migration("202607201600");
        Flyway flyway = flywayInfo(current, new MigrationInfo[0]);
        KodaSchemaHealthIndicator indicator = new KodaSchemaHealthIndicator(flyway);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
            .containsEntry("schema", "current")
            .containsEntry("currentVersion", "202607201600");
    }

    @Test
    void reportsOutOfServiceWhenMigrationsArePending() {
        MigrationInfo current = migration("202607201500");
        MigrationInfo pending = migration("202607201600");
        Flyway flyway = flywayInfo(current, new MigrationInfo[] { pending });
        KodaSchemaHealthIndicator indicator = new KodaSchemaHealthIndicator(flyway);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails())
            .containsEntry("schema", "pending_migrations")
            .containsEntry("currentVersion", "202607201500")
            .containsEntry("pendingCount", 1);
        assertThat(health.getDetails().get("pendingVersions")).isEqualTo(java.util.List.of("202607201600"));
    }

    @Test
    void reportsDownWhenSchemaIsNotInitialized() {
        Flyway flyway = flywayInfo(null, new MigrationInfo[0]);
        KodaSchemaHealthIndicator indicator = new KodaSchemaHealthIndicator(flyway);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("schema", "not_initialized");
    }

    @Test
    void reportsDownWithoutLeakingExceptionMessageWhenFlywayFails() {
        Flyway flyway = mock(Flyway.class);
        when(flyway.info()).thenThrow(new FlywayException("jdbc:postgresql://private-host:5432/koda_platform"));
        KodaSchemaHealthIndicator indicator = new KodaSchemaHealthIndicator(flyway);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
            .containsEntry("schema", "unavailable")
            .containsEntry("errorType", "FlywayException");
        assertThat(health.getDetails().toString()).doesNotContain("private-host");
    }

    private Flyway flywayInfo(MigrationInfo current, MigrationInfo[] pending) {
        MigrationInfoService info = mock(MigrationInfoService.class);
        when(info.current()).thenReturn(current);
        when(info.pending()).thenReturn(pending);

        Flyway flyway = mock(Flyway.class);
        when(flyway.info()).thenReturn(info);
        return flyway;
    }

    private MigrationInfo migration(String version) {
        MigrationInfo migration = mock(MigrationInfo.class);
        when(migration.getVersion()).thenReturn(MigrationVersion.fromVersion(version));
        return migration;
    }
}
