package com.koda.platform.platform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogServiceTest {

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID userId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();
    private final UUID presentationId = UUID.randomUUID();
    private final FakeCatalogRepository repository = new FakeCatalogRepository();
    private final FakeCurrentTenantProvider currentTenantProvider = new FakeCurrentTenantProvider();
    private final CatalogRequestMetadata metadata = new CatalogRequestMetadata("127.0.0.1", "JUnit");
    private CatalogService service;

    @BeforeEach
    void setUp() {
        service = new CatalogService(repository, currentTenantProvider);
        currentTenantProvider.context = Optional.of(new TenantContext(
            tenantId,
            userId,
            Set.of("TENANT_ADMIN"),
            Set.of(
                "brands:read", "brands:create", "brands:update", "brands:delete",
                "categories:read", "categories:create", "categories:update", "categories:delete",
                "units:read", "units:create", "units:update", "units:delete",
                "presentations:read", "presentations:create", "presentations:update", "presentations:delete",
                "products:read", "products:create", "products:update", "products:delete"
            ),
            false
        ));
        repository.units.add(new UnitOfMeasure(unitId, tenantId, "UN", "Unidad", "u", 0, true, 0, Instant.parse("2026-07-17T15:00:00Z")));
        repository.presentations.add(new ProductPresentation(presentationId, tenantId, unitId, "UN", "Unidad", BigDecimal.ONE, true, 0,
            Instant.parse("2026-07-17T15:00:00Z")));
    }

    @Test
    void createBrandNormalizesCodeAndRecordsAudit() {
        Brand brand = service.createBrand(new CreateBrandCommand("  acme  ", " ACME ", "  ", null), metadata);

        assertThat(brand.code()).isEqualTo("ACME");
        assertThat(brand.name()).isEqualTo("ACME");
        assertThat(brand.description()).isNull();
        assertThat(brand.active()).isTrue();
        assertThat(repository.auditActions).contains("catalog.brand.create:brand");
    }

    @Test
    void createBrandRejectsMissingPermission() {
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of(), Set.of("brands:read"), false));

        assertThatThrownBy(() -> service.createBrand(new CreateBrandCommand("ACME", "ACME", null, true), metadata))
            .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void createPresentationRequiresActiveUnit() {
        UUID missingUnit = UUID.randomUUID();

        assertThatThrownBy(() -> service.createPresentation(new CreateProductPresentationCommand(missingUnit, "BOX", "Caja", BigDecimal.TEN, true), metadata))
            .isInstanceOf(CatalogReferenceNotFoundException.class)
            .hasMessage("Catalog reference not found");
    }

    @Test
    void createProductAllowsOptionalBrandAndRequiresDefaultPresentation() {
        Product product = service.createProduct(new CreateProductCommand(
            " sku-001 ",
            "Alimento Balanceado",
            null,
            null,
            null,
            null,
            unitId,
            presentationId,
            "good",
            "active",
            null
        ), metadata);

        assertThat(product.sku()).isEqualTo("SKU-001");
        assertThat(product.brandId()).isNull();
        assertThat(product.categoryId()).isNull();
        assertThat(product.defaultPresentationId()).isEqualTo(presentationId);
        assertThat(product.allowNegativeStock()).isFalse();
        assertThat(repository.auditActions).contains("catalog.product.create:product");
    }

    @Test
    void updateProductRejectsStaleVersion() {
        Product product = service.createProduct(new CreateProductCommand("SKU-002", "Producto", null, null, null, null, unitId, presentationId,
            "GOOD", "ACTIVE", true), metadata);

        assertThatThrownBy(() -> service.updateProduct(product.id(), new UpdateProductCommand(99, "SKU-002", "Producto", null, null, null, null,
            unitId, presentationId, "GOOD", "ACTIVE", true), metadata))
            .isInstanceOf(CatalogVersionConflictException.class);
    }

    @Test
    void deleteBrandUsesSoftDeleteWithVersion() {
        Brand brand = service.createBrand(new CreateBrandCommand("BR", "Marca", null, true), metadata);

        service.deleteBrand(brand.id(), brand.version(), metadata);

        assertThat(repository.findBrandById(tenantId, brand.id())).isEmpty();
        assertThat(repository.auditActions).contains("catalog.brand.delete:brand");
    }

    private final class FakeCurrentTenantProvider implements CurrentTenantProvider {
        private Optional<TenantContext> context = Optional.empty();

        @Override
        public Optional<TenantContext> currentContext() {
            return context;
        }
    }

    private final class FakeCatalogRepository implements CatalogRepository {
        private final List<Brand> brands = new ArrayList<>();
        private final List<Category> categories = new ArrayList<>();
        private final List<UnitOfMeasure> units = new ArrayList<>();
        private final List<ProductPresentation> presentations = new ArrayList<>();
        private final List<Product> products = new ArrayList<>();
        private final List<String> auditActions = new ArrayList<>();

        @Override
        public List<Brand> listBrands(TenantId tenantId) {
            return brands.stream().filter(item -> item.tenantId().equals(tenantId)).toList();
        }

        @Override
        public Optional<Brand> findBrandById(TenantId tenantId, UUID id) {
            return brands.stream().filter(item -> item.tenantId().equals(tenantId) && item.id().equals(id)).findFirst();
        }

        @Override
        public Brand createBrand(TenantId tenantId, UUID actorUserId, CreateBrandCommand command) {
            Brand brand = new Brand(UUID.randomUUID(), tenantId, command.code(), command.name(), command.description(), command.active(), 0,
                Instant.parse("2026-07-17T16:00:00Z"));
            brands.add(brand);
            return brand;
        }

        @Override
        public Optional<Brand> updateBrand(TenantId tenantId, UUID id, UUID actorUserId, UpdateBrandCommand command) {
            Optional<Brand> existing = findBrandById(tenantId, id);
            if (existing.isEmpty() || existing.get().version() != command.version()) {
                return Optional.empty();
            }
            brands.remove(existing.get());
            Brand updated = new Brand(id, tenantId, command.code(), command.name(), command.description(), command.active(), command.version() + 1,
                Instant.parse("2026-07-17T16:00:00Z"));
            brands.add(updated);
            return Optional.of(updated);
        }

        @Override
        public boolean deleteBrand(TenantId tenantId, UUID id, UUID actorUserId, long version) {
            Optional<Brand> existing = findBrandById(tenantId, id);
            return existing.filter(brand -> brand.version() == version).map(brands::remove).orElse(false);
        }

        @Override
        public boolean existsActiveBrand(TenantId tenantId, UUID id) {
            return findBrandById(tenantId, id).filter(Brand::active).isPresent();
        }

        @Override
        public List<Category> listCategories(TenantId tenantId) {
            return categories.stream().filter(item -> item.tenantId().equals(tenantId)).toList();
        }

        @Override
        public Optional<Category> findCategoryById(TenantId tenantId, UUID id) {
            return categories.stream().filter(item -> item.tenantId().equals(tenantId) && item.id().equals(id)).findFirst();
        }

        @Override
        public Category createCategory(TenantId tenantId, UUID actorUserId, CreateCategoryCommand command) {
            Category category = new Category(UUID.randomUUID(), tenantId, command.code(), command.name(), command.description(), command.active(), 0,
                Instant.parse("2026-07-17T16:00:00Z"));
            categories.add(category);
            return category;
        }

        @Override
        public Optional<Category> updateCategory(TenantId tenantId, UUID id, UUID actorUserId, UpdateCategoryCommand command) {
            return Optional.empty();
        }

        @Override
        public boolean deleteCategory(TenantId tenantId, UUID id, UUID actorUserId, long version) {
            return false;
        }

        @Override
        public boolean existsActiveCategory(TenantId tenantId, UUID id) {
            return findCategoryById(tenantId, id).filter(Category::active).isPresent();
        }

        @Override
        public List<UnitOfMeasure> listUnits(TenantId tenantId) {
            return units.stream().filter(item -> item.tenantId().equals(tenantId)).toList();
        }

        @Override
        public Optional<UnitOfMeasure> findUnitById(TenantId tenantId, UUID id) {
            return units.stream().filter(item -> item.tenantId().equals(tenantId) && item.id().equals(id)).findFirst();
        }

        @Override
        public UnitOfMeasure createUnit(TenantId tenantId, UUID actorUserId, CreateUnitOfMeasureCommand command) {
            UnitOfMeasure unit = new UnitOfMeasure(UUID.randomUUID(), tenantId, command.code(), command.name(), command.symbol(),
                command.decimalPrecision(), command.active(), 0, Instant.parse("2026-07-17T16:00:00Z"));
            units.add(unit);
            return unit;
        }

        @Override
        public Optional<UnitOfMeasure> updateUnit(TenantId tenantId, UUID id, UUID actorUserId, UpdateUnitOfMeasureCommand command) {
            return Optional.empty();
        }

        @Override
        public boolean deleteUnit(TenantId tenantId, UUID id, UUID actorUserId, long version) {
            return false;
        }

        @Override
        public boolean existsActiveUnit(TenantId tenantId, UUID id) {
            return findUnitById(tenantId, id).filter(UnitOfMeasure::active).isPresent();
        }

        @Override
        public List<ProductPresentation> listPresentations(TenantId tenantId) {
            return presentations.stream().filter(item -> item.tenantId().equals(tenantId)).toList();
        }

        @Override
        public Optional<ProductPresentation> findPresentationById(TenantId tenantId, UUID id) {
            return presentations.stream().filter(item -> item.tenantId().equals(tenantId) && item.id().equals(id)).findFirst();
        }

        @Override
        public ProductPresentation createPresentation(TenantId tenantId, UUID actorUserId, CreateProductPresentationCommand command) {
            ProductPresentation presentation = new ProductPresentation(UUID.randomUUID(), tenantId, command.unitId(), command.code(), command.name(),
                command.quantity(), command.active(), 0, Instant.parse("2026-07-17T16:00:00Z"));
            presentations.add(presentation);
            return presentation;
        }

        @Override
        public Optional<ProductPresentation> updatePresentation(TenantId tenantId, UUID id, UUID actorUserId, UpdateProductPresentationCommand command) {
            return Optional.empty();
        }

        @Override
        public boolean deletePresentation(TenantId tenantId, UUID id, UUID actorUserId, long version) {
            return false;
        }

        @Override
        public boolean existsActivePresentation(TenantId tenantId, UUID id) {
            return findPresentationById(tenantId, id).filter(ProductPresentation::active).isPresent();
        }

        @Override
        public List<Product> listProducts(TenantId tenantId) {
            return products.stream().filter(item -> item.tenantId().equals(tenantId)).toList();
        }

        @Override
        public Optional<Product> findProductById(TenantId tenantId, UUID id) {
            return products.stream().filter(item -> item.tenantId().equals(tenantId) && item.id().equals(id)).findFirst();
        }

        @Override
        public Product createProduct(TenantId tenantId, UUID actorUserId, CreateProductCommand command) {
            Product product = new Product(UUID.randomUUID(), tenantId, command.sku(), command.name(), command.description(), command.barcode(),
                command.brandId(), command.categoryId(), command.baseUnitId(), command.defaultPresentationId(), command.productType(), command.status(),
                command.stockTrackingEnabled(), false, 0, Instant.parse("2026-07-17T16:00:00Z"));
            products.add(product);
            return product;
        }

        @Override
        public Optional<Product> updateProduct(TenantId tenantId, UUID id, UUID actorUserId, UpdateProductCommand command) {
            Optional<Product> existing = findProductById(tenantId, id);
            if (existing.isEmpty() || existing.get().version() != command.version()) {
                return Optional.empty();
            }
            products.remove(existing.get());
            Product updated = new Product(id, tenantId, command.sku(), command.name(), command.description(), command.barcode(), command.brandId(),
                command.categoryId(), command.baseUnitId(), command.defaultPresentationId(), command.productType(), command.status(),
                command.stockTrackingEnabled(), false, command.version() + 1, Instant.parse("2026-07-17T16:00:00Z"));
            products.add(updated);
            return Optional.of(updated);
        }

        @Override
        public boolean deleteProduct(TenantId tenantId, UUID id, UUID actorUserId, long version) {
            Optional<Product> existing = findProductById(tenantId, id);
            return existing.filter(product -> product.version() == version).map(products::remove).orElse(false);
        }

        @Override
        public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId, String outcome,
                                     CatalogRequestMetadata metadata, Map<String, Object> details) {
            auditActions.add(action + ":" + resourceType);
        }
    }
}