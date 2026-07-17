package com.koda.platform.platform.catalog.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.catalog.application.Brand;
import com.koda.platform.platform.catalog.application.CatalogRepository;
import com.koda.platform.platform.catalog.application.CatalogRequestMetadata;
import com.koda.platform.platform.catalog.application.Category;
import com.koda.platform.platform.catalog.application.CreateBrandCommand;
import com.koda.platform.platform.catalog.application.CreateCategoryCommand;
import com.koda.platform.platform.catalog.application.CreateProductCommand;
import com.koda.platform.platform.catalog.application.CreateProductPresentationCommand;
import com.koda.platform.platform.catalog.application.CreateUnitOfMeasureCommand;
import com.koda.platform.platform.catalog.application.Product;
import com.koda.platform.platform.catalog.application.ProductPresentation;
import com.koda.platform.platform.catalog.application.UnitOfMeasure;
import com.koda.platform.platform.catalog.application.UpdateBrandCommand;
import com.koda.platform.platform.catalog.application.UpdateCategoryCommand;
import com.koda.platform.platform.catalog.application.UpdateProductCommand;
import com.koda.platform.platform.catalog.application.UpdateProductPresentationCommand;
import com.koda.platform.platform.catalog.application.UpdateUnitOfMeasureCommand;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "koda.catalogs.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcCatalogRepository implements CatalogRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcCatalogRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Brand> listBrands(TenantId tenantId) {
        return jdbcTemplate.query(brandSelect() + " WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY name", this::mapBrand, tenantId.value());
    }

    @Override
    public Optional<Brand> findBrandById(TenantId tenantId, UUID id) {
        return jdbcTemplate.query(brandSelect() + " WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL", this::mapBrand, tenantId.value(), id)
            .stream().findFirst();
    }

    @Override
    public Brand createBrand(TenantId tenantId, UUID actorUserId, CreateBrandCommand command) {
        String sql = """
            INSERT INTO brands (tenant_id, code, name, description, is_active, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id, tenant_id, code, name, description, is_active, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapBrand, tenantId.value(), command.code(), command.name(), command.description(), command.active(), actorUserId, actorUserId).getFirst();
    }

    @Override
    public Optional<Brand> updateBrand(TenantId tenantId, UUID id, UUID actorUserId, UpdateBrandCommand command) {
        String sql = """
            UPDATE brands
            SET code = ?, name = ?, description = ?, is_active = ?, updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND deleted_at IS NULL
            RETURNING id, tenant_id, code, name, description, is_active, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapBrand, command.code(), command.name(), command.description(), command.active(), actorUserId, tenantId.value(), id, command.version())
            .stream().findFirst();
    }

    @Override
    public boolean deleteBrand(TenantId tenantId, UUID id, UUID actorUserId, long version) {
        return softDelete("brands", tenantId, id, actorUserId, version);
    }

    @Override
    public boolean existsActiveBrand(TenantId tenantId, UUID id) {
        return existsActive("brands", tenantId, id);
    }

    @Override
    public List<Category> listCategories(TenantId tenantId) {
        return jdbcTemplate.query(categorySelect() + " WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY name", this::mapCategory, tenantId.value());
    }

    @Override
    public Optional<Category> findCategoryById(TenantId tenantId, UUID id) {
        return jdbcTemplate.query(categorySelect() + " WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL", this::mapCategory, tenantId.value(), id)
            .stream().findFirst();
    }

    @Override
    public Category createCategory(TenantId tenantId, UUID actorUserId, CreateCategoryCommand command) {
        String sql = """
            INSERT INTO categories (tenant_id, code, name, description, is_active, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id, tenant_id, code, name, description, is_active, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapCategory, tenantId.value(), command.code(), command.name(), command.description(), command.active(), actorUserId, actorUserId).getFirst();
    }

    @Override
    public Optional<Category> updateCategory(TenantId tenantId, UUID id, UUID actorUserId, UpdateCategoryCommand command) {
        String sql = """
            UPDATE categories
            SET code = ?, name = ?, description = ?, is_active = ?, parent_id = NULL, updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND deleted_at IS NULL
            RETURNING id, tenant_id, code, name, description, is_active, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapCategory, command.code(), command.name(), command.description(), command.active(), actorUserId, tenantId.value(), id, command.version())
            .stream().findFirst();
    }

    @Override
    public boolean deleteCategory(TenantId tenantId, UUID id, UUID actorUserId, long version) {
        return softDelete("categories", tenantId, id, actorUserId, version);
    }

    @Override
    public boolean existsActiveCategory(TenantId tenantId, UUID id) {
        return existsActive("categories", tenantId, id);
    }

    @Override
    public List<UnitOfMeasure> listUnits(TenantId tenantId) {
        return jdbcTemplate.query(unitSelect() + " WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY name", this::mapUnit, tenantId.value());
    }

    @Override
    public Optional<UnitOfMeasure> findUnitById(TenantId tenantId, UUID id) {
        return jdbcTemplate.query(unitSelect() + " WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL", this::mapUnit, tenantId.value(), id)
            .stream().findFirst();
    }

    @Override
    public UnitOfMeasure createUnit(TenantId tenantId, UUID actorUserId, CreateUnitOfMeasureCommand command) {
        String sql = """
            INSERT INTO units_of_measure (tenant_id, code, name, symbol, decimal_precision, is_active, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, tenant_id, code, name, symbol, decimal_precision, is_active, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapUnit, tenantId.value(), command.code(), command.name(), command.symbol(), command.decimalPrecision(), command.active(), actorUserId, actorUserId).getFirst();
    }

    @Override
    public Optional<UnitOfMeasure> updateUnit(TenantId tenantId, UUID id, UUID actorUserId, UpdateUnitOfMeasureCommand command) {
        String sql = """
            UPDATE units_of_measure
            SET code = ?, name = ?, symbol = ?, decimal_precision = ?, is_active = ?, updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND deleted_at IS NULL
            RETURNING id, tenant_id, code, name, symbol, decimal_precision, is_active, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapUnit, command.code(), command.name(), command.symbol(), command.decimalPrecision(), command.active(), actorUserId, tenantId.value(), id, command.version())
            .stream().findFirst();
    }

    @Override
    public boolean deleteUnit(TenantId tenantId, UUID id, UUID actorUserId, long version) {
        return softDelete("units_of_measure", tenantId, id, actorUserId, version);
    }

    @Override
    public boolean existsActiveUnit(TenantId tenantId, UUID id) {
        return existsActive("units_of_measure", tenantId, id);
    }

    @Override
    public List<ProductPresentation> listPresentations(TenantId tenantId) {
        return jdbcTemplate.query(presentationSelect() + " WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY name", this::mapPresentation, tenantId.value());
    }

    @Override
    public Optional<ProductPresentation> findPresentationById(TenantId tenantId, UUID id) {
        return jdbcTemplate.query(presentationSelect() + " WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL", this::mapPresentation, tenantId.value(), id)
            .stream().findFirst();
    }

    @Override
    public ProductPresentation createPresentation(TenantId tenantId, UUID actorUserId, CreateProductPresentationCommand command) {
        String sql = """
            INSERT INTO product_presentations (tenant_id, unit_id, code, name, quantity, is_active, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, tenant_id, unit_id, code, name, quantity, is_active, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapPresentation, tenantId.value(), command.unitId(), command.code(), command.name(), command.quantity(), command.active(), actorUserId, actorUserId).getFirst();
    }

    @Override
    public Optional<ProductPresentation> updatePresentation(TenantId tenantId, UUID id, UUID actorUserId, UpdateProductPresentationCommand command) {
        String sql = """
            UPDATE product_presentations
            SET unit_id = ?, code = ?, name = ?, quantity = ?, is_active = ?, updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND deleted_at IS NULL
            RETURNING id, tenant_id, unit_id, code, name, quantity, is_active, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapPresentation, command.unitId(), command.code(), command.name(), command.quantity(), command.active(), actorUserId, tenantId.value(), id, command.version())
            .stream().findFirst();
    }

    @Override
    public boolean deletePresentation(TenantId tenantId, UUID id, UUID actorUserId, long version) {
        return softDelete("product_presentations", tenantId, id, actorUserId, version);
    }

    @Override
    public boolean existsActivePresentation(TenantId tenantId, UUID id) {
        return existsActive("product_presentations", tenantId, id);
    }

    @Override
    public List<Product> listProducts(TenantId tenantId) {
        return jdbcTemplate.query(productSelect() + " WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY name", this::mapProduct, tenantId.value());
    }

    @Override
    public Optional<Product> findProductById(TenantId tenantId, UUID id) {
        return jdbcTemplate.query(productSelect() + " WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL", this::mapProduct, tenantId.value(), id)
            .stream().findFirst();
    }

    @Override
    public Product createProduct(TenantId tenantId, UUID actorUserId, CreateProductCommand command) {
        String sql = """
            INSERT INTO products (
                tenant_id, sku, name, description, barcode, brand_id, category_id, base_unit_id, default_presentation_id,
                product_type, status, stock_tracking_enabled, allow_negative_stock, created_by, updated_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?, ?)
            RETURNING id, tenant_id, sku, name, description, barcode, brand_id, category_id, base_unit_id, default_presentation_id,
                product_type, status, stock_tracking_enabled, allow_negative_stock, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapProduct,
            tenantId.value(), command.sku(), command.name(), command.description(), command.barcode(), command.brandId(), command.categoryId(),
            command.baseUnitId(), command.defaultPresentationId(), command.productType(), command.status(), command.stockTrackingEnabled(), actorUserId, actorUserId
        ).getFirst();
    }

    @Override
    public Optional<Product> updateProduct(TenantId tenantId, UUID id, UUID actorUserId, UpdateProductCommand command) {
        String sql = """
            UPDATE products
            SET sku = ?, name = ?, description = ?, barcode = ?, brand_id = ?, category_id = ?, base_unit_id = ?, default_presentation_id = ?,
                product_type = ?, status = ?, stock_tracking_enabled = ?, allow_negative_stock = false, updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND deleted_at IS NULL
            RETURNING id, tenant_id, sku, name, description, barcode, brand_id, category_id, base_unit_id, default_presentation_id,
                product_type, status, stock_tracking_enabled, allow_negative_stock, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapProduct,
            command.sku(), command.name(), command.description(), command.barcode(), command.brandId(), command.categoryId(), command.baseUnitId(),
            command.defaultPresentationId(), command.productType(), command.status(), command.stockTrackingEnabled(), actorUserId, tenantId.value(), id, command.version()
        ).stream().findFirst();
    }

    @Override
    public boolean deleteProduct(TenantId tenantId, UUID id, UUID actorUserId, long version) {
        return softDelete("products", tenantId, id, actorUserId, version);
    }

    @Override
    public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                                 String outcome, CatalogRequestMetadata metadata, Map<String, Object> details) {
        String sql = """
            INSERT INTO audit_events (
                tenant_id, actor_user_id, actor_type, action, resource_type, resource_id, outcome, source_ip, user_agent, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS inet), ?, CAST(? AS jsonb))
            """;
        jdbcTemplate.update(sql,
            tenantId == null ? null : tenantId.value(),
            actorUserId,
            actorUserId == null ? "SYSTEM" : "USER",
            action,
            resourceType,
            resourceId,
            outcome,
            metadata == null ? null : metadata.sourceIp(),
            metadata == null ? null : metadata.userAgent(),
            toJson(details)
        );
    }

    private boolean softDelete(String tableName, TenantId tenantId, UUID id, UUID actorUserId, long version) {
        String sql = """
            UPDATE %s
            SET deleted_at = now(), updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND deleted_at IS NULL
            """.formatted(tableName);
        return jdbcTemplate.update(sql, actorUserId, tenantId.value(), id, version) > 0;
    }

    private boolean existsActive(String tableName, TenantId tenantId, UUID id) {
        String sql = "SELECT count(*) FROM %s WHERE tenant_id = ? AND id = ? AND is_active = true AND deleted_at IS NULL".formatted(tableName);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId.value(), id);
        return count != null && count > 0;
    }

    private String brandSelect() {
        return "SELECT id, tenant_id, code, name, description, is_active, version, updated_at FROM brands";
    }

    private String categorySelect() {
        return "SELECT id, tenant_id, code, name, description, is_active, version, updated_at FROM categories";
    }

    private String unitSelect() {
        return "SELECT id, tenant_id, code, name, symbol, decimal_precision, is_active, version, updated_at FROM units_of_measure";
    }

    private String presentationSelect() {
        return "SELECT id, tenant_id, unit_id, code, name, quantity, is_active, version, updated_at FROM product_presentations";
    }

    private String productSelect() {
        return """
            SELECT id, tenant_id, sku, name, description, barcode, brand_id, category_id, base_unit_id, default_presentation_id,
                   product_type, status, stock_tracking_enabled, allow_negative_stock, version, updated_at
            FROM products
            """;
    }

    private Brand mapBrand(ResultSet rs, int rowNum) throws SQLException {
        return new Brand(rs.getObject("id", UUID.class), TenantId.from(rs.getObject("tenant_id", UUID.class)), rs.getString("code"),
            rs.getString("name"), rs.getString("description"), rs.getBoolean("is_active"), rs.getLong("version"), rs.getTimestamp("updated_at").toInstant());
    }

    private Category mapCategory(ResultSet rs, int rowNum) throws SQLException {
        return new Category(rs.getObject("id", UUID.class), TenantId.from(rs.getObject("tenant_id", UUID.class)), rs.getString("code"),
            rs.getString("name"), rs.getString("description"), rs.getBoolean("is_active"), rs.getLong("version"), rs.getTimestamp("updated_at").toInstant());
    }

    private UnitOfMeasure mapUnit(ResultSet rs, int rowNum) throws SQLException {
        return new UnitOfMeasure(rs.getObject("id", UUID.class), TenantId.from(rs.getObject("tenant_id", UUID.class)), rs.getString("code"),
            rs.getString("name"), rs.getString("symbol"), rs.getInt("decimal_precision"), rs.getBoolean("is_active"), rs.getLong("version"),
            rs.getTimestamp("updated_at").toInstant());
    }

    private ProductPresentation mapPresentation(ResultSet rs, int rowNum) throws SQLException {
        return new ProductPresentation(rs.getObject("id", UUID.class), TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getObject("unit_id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getBigDecimal("quantity"),
            rs.getBoolean("is_active"), rs.getLong("version"), rs.getTimestamp("updated_at").toInstant());
    }

    private Product mapProduct(ResultSet rs, int rowNum) throws SQLException {
        return new Product(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getString("sku"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("barcode"),
            rs.getObject("brand_id", UUID.class),
            rs.getObject("category_id", UUID.class),
            rs.getObject("base_unit_id", UUID.class),
            rs.getObject("default_presentation_id", UUID.class),
            rs.getString("product_type"),
            rs.getString("status"),
            rs.getBoolean("stock_tracking_enabled"),
            rs.getBoolean("allow_negative_stock"),
            rs.getLong("version"),
            rs.getTimestamp("updated_at").toInstant()
        );
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details == null ? Map.of() : details);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize audit metadata", exception);
        }
    }
}