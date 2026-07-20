package com.koda.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class FlywayPostgresqlIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
        .withDatabaseName("koda_platform_it")
        .withUsername("koda")
        .withPassword("koda_dev_password");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration")
            .load()
            .migrate();
    }

    @Test
    void flywayBuildsSprintOneSchemaAndSeedDataOnPostgresql17() throws SQLException {
        assertThat(queryForString("select version from flyway_schema_history where success = true order by installed_rank desc limit 1"))
            .isEqualTo("202607201210");
        assertThat(queryForInt("select count(*) from tenants")).isEqualTo(1);
        assertThat(queryForInt("select count(*) from platform_modules")).isEqualTo(8);
        assertThat(queryForInt("select count(*) from permissions")).isEqualTo(61);
        assertThat(queryForInt("select count(*) from roles")).isEqualTo(7);
        assertThat(queryForInt("select count(*) from role_permissions")).isEqualTo(158);
        assertThat(queryForString("select code from warehouses where tenant_id = '00000000-0000-4000-8000-000000000001'"))
            .isEqualTo("PRINCIPAL");
        assertThat(queryForString("select code from cash_registers where tenant_id = '00000000-0000-4000-8000-000000000001'"))
            .isEqualTo("CAJA_PRINCIPAL");
        assertThat(queryForString("""
            select p.legal_name
            from business_partners p
            join business_partner_roles r on r.tenant_id = p.tenant_id and r.business_partner_id = p.id
            where p.tenant_id = '00000000-0000-4000-8000-000000000001'
              and r.role_type = 'CUSTOMER'
              and p.is_system = true
            """))
            .isEqualTo("Consumidor Final");
    }

    @Test
    void tenantScopedTablesKeepTenantColumnInPostgresqlSchema() throws SQLException {
        List<String> tenantScopedTables = List.of(
            "company_settings",
            "tenant_product_entitlements",
            "tenant_module_entitlements",
            "branches",
            "warehouses",
            "roles",
            "tenant_memberships",
            "tenant_membership_roles",
            "refresh_tokens",
            "brands",
            "categories",
            "units_of_measure",
            "product_presentations",
            "products",
            "stock_balances",
            "stock_movements",
            "audit_events",
            "business_partners",
            "business_partner_roles",
            "cash_registers",
            "cash_sessions",
            "cash_movements",
            "sales_number_sequences",
            "sales_orders",
            "sales_order_items"
        );

        for (String tableName : tenantScopedTables) {
            assertThat(hasColumn(tableName, "tenant_id"))
                .as(tableName + " must keep tenant_id")
                .isTrue();
        }
    }

    @Test
    void stockLedgerMigrationAddsTraceabilityColumns() throws SQLException {
        assertThat(hasColumn("stock_movements", "quantity_before")).isTrue();
        assertThat(hasColumn("stock_movements", "quantity_after")).isTrue();
        assertThat(hasColumn("stock_movements", "quantity_delta")).isTrue();
    }


    @Test
    void salesMigrationAddsInternalNumberingAndPermissions() throws SQLException {
        assertThat(hasColumn("sales_orders", "sale_number")).isTrue();
        assertThat(hasColumn("sales_order_items", "stock_movement_id")).isTrue();
        assertThat(queryForInt("select count(*) from permissions where code like 'sales:%'")).isEqualTo(6);
    }
    private static int queryForInt(String sql) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static String queryForString(String sql) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private static boolean hasColumn(String tableName, String columnName) throws SQLException {
        String sql = """
            select count(*)
            from information_schema.columns
            where table_schema = 'public'
              and table_name = '%s'
              and column_name = '%s'
            """.formatted(tableName, columnName);
        return queryForInt(sql) == 1;
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}