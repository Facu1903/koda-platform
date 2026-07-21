package com.koda.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.licensing.infrastructure.JdbcTenantCapabilitiesRepository;
import com.koda.platform.platform.licensing.infrastructure.JdbcTenantLicenseAdministrationRepository;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class FlywayPostgresqlIT {

    private static final String KODA_TENANT_ID = "00000000-0000-4000-8000-000000000001";
    private static final String KODA_ERP_PRODUCT_ID = "10000000-0000-4000-8000-000000000001";
    private static final String KODA_PILOT_PLAN_ID = "21000000-0000-4000-8000-000000000001";
    private static final String KODA_PILOT_SUBSCRIPTION_ID = "22000000-0000-4000-8000-000000000001";

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
    void flywayBuildsCurrentSchemaAndSeedDataOnPostgresql17() throws SQLException {
        assertThat(queryForString("select version from flyway_schema_history where success = true order by installed_rank desc limit 1"))
            .isEqualTo("202607201600");
        assertThat(queryForInt("select count(*) from tenants")).isEqualTo(1);
        assertThat(queryForInt("select count(*) from platform_modules")).isEqualTo(10);
        assertThat(queryForInt("select count(*) from permissions")).isEqualTo(70);
        assertThat(queryForInt("select count(*) from roles")).isEqualTo(7);
        assertThat(queryForInt("select count(*) from role_permissions")).isEqualTo(189);
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
            "tenant_product_subscriptions",
            "tenant_limit_overrides",
            "tenant_feature_flags",
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
            "sales_order_items",
            "purchase_number_sequences",
            "purchase_orders",
            "purchase_order_items"
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

    @Test
    void purchasesMigrationAddsInternalNumberingAndPermissions() throws SQLException {
        assertThat(hasColumn("purchase_orders", "purchase_number")).isTrue();
        assertThat(hasColumn("purchase_orders", "supplier_document_number")).isTrue();
        assertThat(hasColumn("purchase_order_items", "stock_movement_id")).isTrue();
        assertThat(queryForInt("select count(*) from permissions where code like 'purchases:%'")).isEqualTo(6);
    }

    @Test
    void reportsMigrationAddsPermissionAndReportingIndexes() throws SQLException {
        assertThat(queryForInt("select count(*) from permissions where code = 'commercial_reports:read'")).isEqualTo(1);
        assertThat(hasIndex("idx_sales_orders_tenant_confirmed_at")).isTrue();
        assertThat(hasIndex("idx_purchase_orders_tenant_confirmed_at")).isTrue();
        assertThat(hasIndex("idx_cash_movements_tenant_occurred_at")).isTrue();
        assertThat(hasIndex("idx_stock_balances_tenant_quantity")).isTrue();
        assertThat(hasIndex("idx_stock_movements_tenant_confirmed_at")).isTrue();
    }

    @Test
    void licensingMigrationAddsPilotPlanSubscriptionsAndCapabilityTables() throws SQLException {
        assertThat(hasColumn("platform_modules", "core_module")).isTrue();
        assertThat(hasColumn("platform_modules", "commercially_toggleable")).isTrue();
        assertThat(queryForInt("select count(*) from platform_modules where core_module = true and commercially_toggleable = false"))
            .isEqualTo(3);
        assertThat(queryForString("select code from product_plans where id = '" + KODA_PILOT_PLAN_ID + "'"))
            .isEqualTo("KODA_PILOT");
        assertThat(queryForInt("select count(*) from product_plan_modules where plan_id = '" + KODA_PILOT_PLAN_ID + "'"))
            .isEqualTo(10);
        assertThat(queryForInt("select count(*) from product_plan_limits where plan_id = '" + KODA_PILOT_PLAN_ID + "' and unlimited = true"))
            .isEqualTo(5);
        assertThat(queryForString("select status from tenant_product_subscriptions where id = '" + KODA_PILOT_SUBSCRIPTION_ID + "'"))
            .isEqualTo("ACTIVE");
        assertThat(queryForInt("select count(*) from tenant_product_entitlements where tenant_id = '" + KODA_TENANT_ID + "' and product_id = '" + KODA_ERP_PRODUCT_ID + "' and status = 'ACTIVE'"))
            .isEqualTo(1);
        assertThat(queryForInt("select count(*) from tenant_module_entitlements where tenant_id = '" + KODA_TENANT_ID + "' and status = 'ACTIVE'"))
            .isEqualTo(10);
        assertThat(hasIndex("uq_tenant_product_subscriptions_current")).isTrue();
        assertThat(hasIndex("idx_tenant_product_subscriptions_tenant_status")).isTrue();
        assertThat(hasIndex("idx_tenant_module_entitlements_tenant_status")).isTrue();
        assertThat(hasIndex("idx_tenant_feature_flags_tenant_product_enabled")).isTrue();
    }

    @Test
    void licenseAdministrationMigrationAddsPlatformPermissions() throws SQLException {
        assertThat(queryForInt("select count(*) from permissions where code like 'license_admin:%'")).isEqualTo(2);
        assertThat(queryForInt("""
            select count(*)
            from role_permissions rp
            join roles r on r.id = rp.role_id
            join permissions p on p.id = rp.permission_id
            where r.code = 'PLATFORM_SUPER_ADMIN'
              and r.scope = 'PLATFORM'
              and p.code like 'license_admin:%'
            """)).isEqualTo(2);
    }

    @Test
    void licenseAdministrationRepositoryReadsKodaPilotLicenseState() throws SQLException {
        JdbcTenantLicenseAdministrationRepository repository = new JdbcTenantLicenseAdministrationRepository(new JdbcTemplate(dataSource()), new ObjectMapper());
        var tenant = repository.findTenant(TenantId.fromString(KODA_TENANT_ID)).orElseThrow();
        var administration = repository.findAdministration(tenant);

        assertThat(administration.tenant().commercialName()).isEqualTo("KODA");
        assertThat(administration.subscriptions()).hasSize(1);
        assertThat(administration.subscriptions().getFirst().planCode()).isEqualTo("KODA_PILOT");
        assertThat(administration.productEntitlements()).hasSize(1);
        assertThat(administration.moduleEntitlements()).hasSize(10);
        assertThat(administration.limitOverrides()).isEmpty();
        assertThat(administration.featureFlags()).isEmpty();
    }

    @Test
    void capabilitiesRepositoryResolvesKodaPilotEffectiveCapabilities() throws SQLException {
        JdbcTenantCapabilitiesRepository repository = new JdbcTenantCapabilitiesRepository(new JdbcTemplate(dataSource()));
        TenantId tenantId = TenantId.fromString(KODA_TENANT_ID);
        Instant calculatedAt = Instant.now();

        assertThat(repository.findEnabledProducts(tenantId, calculatedAt))
            .hasSize(1)
            .first()
            .satisfies(product -> {
                assertThat(product.code()).isEqualTo("KODA_ERP");
                assertThat(product.planCode()).isEqualTo("KODA_PILOT");
                assertThat(product.enabled()).isTrue();
            });
        assertThat(repository.findEnabledModules(tenantId, calculatedAt)).hasSize(10);
        assertThat(repository.findEffectiveLimits(tenantId, calculatedAt)).hasSize(5);
        assertThat(repository.findEffectiveFeatureFlags(tenantId, calculatedAt)).isEmpty();
    }

    @Test
    void licenseAccessRepositoryResolvesKodaPilotProductAndModuleGuards() throws SQLException {
        JdbcTenantCapabilitiesRepository repository = new JdbcTenantCapabilitiesRepository(new JdbcTemplate(dataSource()));
        TenantId tenantId = TenantId.fromString(KODA_TENANT_ID);
        Instant calculatedAt = Instant.now();

        assertThat(repository.isProductEnabled(tenantId, "KODA_ERP", calculatedAt)).isTrue();
        assertThat(repository.isProductEnabled(tenantId, "KODA_POS", calculatedAt)).isFalse();
        assertThat(repository.isModuleEnabled(tenantId, "KODA_ERP", "SALES", calculatedAt)).isTrue();
        assertThat(repository.isModuleEnabled(tenantId, "KODA_ERP", "UNKNOWN", calculatedAt)).isFalse();
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

    private static boolean hasIndex(String indexName) throws SQLException {
        String sql = """
            select count(*)
            from pg_indexes
            where schemaname = 'public'
              and indexname = '%s'
            """.formatted(indexName);
        return queryForInt(sql) == 1;
    }

    private static DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}