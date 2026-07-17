package com.koda.platform.platform.catalog.application;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final CatalogRepository repository;
    private final CurrentTenantProvider currentTenantProvider;

    public CatalogService(CatalogRepository repository, CurrentTenantProvider currentTenantProvider) {
        this.repository = repository;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Transactional(readOnly = true)
    public List<Brand> listBrands() {
        TenantContext context = requirePermission("brands:read");
        return repository.listBrands(context.tenantId());
    }

    @Transactional(readOnly = true)
    public Brand getBrand(UUID id) {
        TenantContext context = requirePermission("brands:read");
        return repository.findBrandById(context.tenantId(), id).orElseThrow(() -> notFound("brand"));
    }

    @Transactional
    public Brand createBrand(CreateBrandCommand command, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("brands:create");
        CreateBrandCommand normalized = normalize(command);
        Brand brand = repository.createBrand(context.tenantId(), context.userId(), normalized);
        audit(context, "catalog.brand.create", "brand", brand.id(), metadata, Map.of("code", brand.code()));
        return brand;
    }

    @Transactional
    public Brand updateBrand(UUID id, UpdateBrandCommand command, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("brands:update");
        ensureExistingBrand(context.tenantId(), id);
        Brand brand = repository.updateBrand(context.tenantId(), id, context.userId(), normalize(command))
            .orElseThrow(() -> conflict("brand"));
        audit(context, "catalog.brand.update", "brand", brand.id(), metadata, Map.of("version", brand.version()));
        return brand;
    }

    @Transactional
    public void deleteBrand(UUID id, long version, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("brands:delete");
        ensureExistingBrand(context.tenantId(), id);
        if (!repository.deleteBrand(context.tenantId(), id, context.userId(), version)) {
            throw conflict("brand");
        }
        audit(context, "catalog.brand.delete", "brand", id, metadata, Map.of("version", version));
    }

    @Transactional(readOnly = true)
    public List<Category> listCategories() {
        TenantContext context = requirePermission("categories:read");
        return repository.listCategories(context.tenantId());
    }

    @Transactional(readOnly = true)
    public Category getCategory(UUID id) {
        TenantContext context = requirePermission("categories:read");
        return repository.findCategoryById(context.tenantId(), id).orElseThrow(() -> notFound("category"));
    }

    @Transactional
    public Category createCategory(CreateCategoryCommand command, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("categories:create");
        Category category = repository.createCategory(context.tenantId(), context.userId(), normalize(command));
        audit(context, "catalog.category.create", "category", category.id(), metadata, Map.of("code", category.code()));
        return category;
    }

    @Transactional
    public Category updateCategory(UUID id, UpdateCategoryCommand command, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("categories:update");
        ensureExistingCategory(context.tenantId(), id);
        Category category = repository.updateCategory(context.tenantId(), id, context.userId(), normalize(command))
            .orElseThrow(() -> conflict("category"));
        audit(context, "catalog.category.update", "category", category.id(), metadata, Map.of("version", category.version()));
        return category;
    }

    @Transactional
    public void deleteCategory(UUID id, long version, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("categories:delete");
        ensureExistingCategory(context.tenantId(), id);
        if (!repository.deleteCategory(context.tenantId(), id, context.userId(), version)) {
            throw conflict("category");
        }
        audit(context, "catalog.category.delete", "category", id, metadata, Map.of("version", version));
    }

    @Transactional(readOnly = true)
    public List<UnitOfMeasure> listUnits() {
        TenantContext context = requirePermission("units:read");
        return repository.listUnits(context.tenantId());
    }

    @Transactional(readOnly = true)
    public UnitOfMeasure getUnit(UUID id) {
        TenantContext context = requirePermission("units:read");
        return repository.findUnitById(context.tenantId(), id).orElseThrow(() -> notFound("unit"));
    }

    @Transactional
    public UnitOfMeasure createUnit(CreateUnitOfMeasureCommand command, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("units:create");
        UnitOfMeasure unit = repository.createUnit(context.tenantId(), context.userId(), normalize(command));
        audit(context, "catalog.unit.create", "unit", unit.id(), metadata, Map.of("code", unit.code()));
        return unit;
    }

    @Transactional
    public UnitOfMeasure updateUnit(UUID id, UpdateUnitOfMeasureCommand command, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("units:update");
        ensureExistingUnit(context.tenantId(), id);
        UnitOfMeasure unit = repository.updateUnit(context.tenantId(), id, context.userId(), normalize(command))
            .orElseThrow(() -> conflict("unit"));
        audit(context, "catalog.unit.update", "unit", unit.id(), metadata, Map.of("version", unit.version()));
        return unit;
    }

    @Transactional
    public void deleteUnit(UUID id, long version, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("units:delete");
        ensureExistingUnit(context.tenantId(), id);
        if (!repository.deleteUnit(context.tenantId(), id, context.userId(), version)) {
            throw conflict("unit");
        }
        audit(context, "catalog.unit.delete", "unit", id, metadata, Map.of("version", version));
    }

    @Transactional(readOnly = true)
    public List<ProductPresentation> listPresentations() {
        TenantContext context = requirePermission("presentations:read");
        return repository.listPresentations(context.tenantId());
    }

    @Transactional(readOnly = true)
    public ProductPresentation getPresentation(UUID id) {
        TenantContext context = requirePermission("presentations:read");
        return repository.findPresentationById(context.tenantId(), id).orElseThrow(() -> notFound("presentation"));
    }

    @Transactional
    public ProductPresentation createPresentation(CreateProductPresentationCommand command, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("presentations:create");
        CreateProductPresentationCommand normalized = normalize(command);
        ensureActiveUnit(context.tenantId(), normalized.unitId());
        ProductPresentation presentation = repository.createPresentation(context.tenantId(), context.userId(), normalized);
        audit(context, "catalog.presentation.create", "presentation", presentation.id(), metadata, Map.of("code", presentation.code()));
        return presentation;
    }

    @Transactional
    public ProductPresentation updatePresentation(UUID id, UpdateProductPresentationCommand command, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("presentations:update");
        ensureExistingPresentation(context.tenantId(), id);
        UpdateProductPresentationCommand normalized = normalize(command);
        ensureActiveUnit(context.tenantId(), normalized.unitId());
        ProductPresentation presentation = repository.updatePresentation(context.tenantId(), id, context.userId(), normalized)
            .orElseThrow(() -> conflict("presentation"));
        audit(context, "catalog.presentation.update", "presentation", presentation.id(), metadata, Map.of("version", presentation.version()));
        return presentation;
    }

    @Transactional
    public void deletePresentation(UUID id, long version, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("presentations:delete");
        ensureExistingPresentation(context.tenantId(), id);
        if (!repository.deletePresentation(context.tenantId(), id, context.userId(), version)) {
            throw conflict("presentation");
        }
        audit(context, "catalog.presentation.delete", "presentation", id, metadata, Map.of("version", version));
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        TenantContext context = requirePermission("products:read");
        return repository.listProducts(context.tenantId());
    }

    @Transactional(readOnly = true)
    public Product getProduct(UUID id) {
        TenantContext context = requirePermission("products:read");
        return repository.findProductById(context.tenantId(), id).orElseThrow(() -> notFound("product"));
    }

    @Transactional
    public Product createProduct(CreateProductCommand command, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("products:create");
        CreateProductCommand normalized = normalize(command);
        validateProductReferences(context.tenantId(), normalized.brandId(), normalized.categoryId(), normalized.baseUnitId(), normalized.defaultPresentationId());
        Product product = repository.createProduct(context.tenantId(), context.userId(), normalized);
        audit(context, "catalog.product.create", "product", product.id(), metadata, Map.of("sku", product.sku()));
        return product;
    }

    @Transactional
    public Product updateProduct(UUID id, UpdateProductCommand command, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("products:update");
        ensureExistingProduct(context.tenantId(), id);
        UpdateProductCommand normalized = normalize(command);
        validateProductReferences(context.tenantId(), normalized.brandId(), normalized.categoryId(), normalized.baseUnitId(), normalized.defaultPresentationId());
        Product product = repository.updateProduct(context.tenantId(), id, context.userId(), normalized)
            .orElseThrow(() -> conflict("product"));
        audit(context, "catalog.product.update", "product", product.id(), metadata, Map.of("version", product.version()));
        return product;
    }

    @Transactional
    public void deleteProduct(UUID id, long version, CatalogRequestMetadata metadata) {
        TenantContext context = requirePermission("products:delete");
        ensureExistingProduct(context.tenantId(), id);
        if (!repository.deleteProduct(context.tenantId(), id, context.userId(), version)) {
            throw conflict("product");
        }
        audit(context, "catalog.product.delete", "product", id, metadata, Map.of("version", version));
    }

    private TenantContext requirePermission(String permission) {
        TenantContext context = currentTenantProvider.requireContext();
        if (context.platformAdmin() || context.hasPermission(permission)) {
            return context;
        }
        throw new PermissionDeniedException(permission);
    }

    private CreateBrandCommand normalize(CreateBrandCommand command) {
        return new CreateBrandCommand(normalizeCode(command.code(), "Code"), required(command.name(), "Name"), trimToNull(command.description()), active(command.active()));
    }

    private UpdateBrandCommand normalize(UpdateBrandCommand command) {
        requireVersion(command.version());
        return new UpdateBrandCommand(command.version(), normalizeCode(command.code(), "Code"), required(command.name(), "Name"), trimToNull(command.description()), active(command.active()));
    }

    private CreateCategoryCommand normalize(CreateCategoryCommand command) {
        return new CreateCategoryCommand(normalizeCode(command.code(), "Code"), required(command.name(), "Name"), trimToNull(command.description()), active(command.active()));
    }

    private UpdateCategoryCommand normalize(UpdateCategoryCommand command) {
        requireVersion(command.version());
        return new UpdateCategoryCommand(command.version(), normalizeCode(command.code(), "Code"), required(command.name(), "Name"), trimToNull(command.description()), active(command.active()));
    }

    private CreateUnitOfMeasureCommand normalize(CreateUnitOfMeasureCommand command) {
        return new CreateUnitOfMeasureCommand(normalizeCode(command.code(), "Code"), required(command.name(), "Name"), required(command.symbol(), "Symbol"), precision(command.decimalPrecision()), active(command.active()));
    }

    private UpdateUnitOfMeasureCommand normalize(UpdateUnitOfMeasureCommand command) {
        requireVersion(command.version());
        return new UpdateUnitOfMeasureCommand(command.version(), normalizeCode(command.code(), "Code"), required(command.name(), "Name"), required(command.symbol(), "Symbol"), precision(command.decimalPrecision()), active(command.active()));
    }

    private CreateProductPresentationCommand normalize(CreateProductPresentationCommand command) {
        return new CreateProductPresentationCommand(required(command.unitId(), "Unit"), normalizeCode(command.code(), "Code"), required(command.name(), "Name"), positive(command.quantity(), "Quantity"), active(command.active()));
    }

    private UpdateProductPresentationCommand normalize(UpdateProductPresentationCommand command) {
        requireVersion(command.version());
        return new UpdateProductPresentationCommand(command.version(), required(command.unitId(), "Unit"), normalizeCode(command.code(), "Code"), required(command.name(), "Name"), positive(command.quantity(), "Quantity"), active(command.active()));
    }

    private CreateProductCommand normalize(CreateProductCommand command) {
        return new CreateProductCommand(
            normalizeCode(command.sku(), "SKU"),
            required(command.name(), "Name"),
            trimToNull(command.description()),
            trimToNull(command.barcode()),
            command.brandId(),
            command.categoryId(),
            required(command.baseUnitId(), "Base unit"),
            required(command.defaultPresentationId(), "Default presentation"),
            productType(command.productType()),
            productStatus(command.status()),
            command.stockTrackingEnabled() == null || command.stockTrackingEnabled()
        );
    }

    private UpdateProductCommand normalize(UpdateProductCommand command) {
        requireVersion(command.version());
        return new UpdateProductCommand(
            command.version(),
            normalizeCode(command.sku(), "SKU"),
            required(command.name(), "Name"),
            trimToNull(command.description()),
            trimToNull(command.barcode()),
            command.brandId(),
            command.categoryId(),
            required(command.baseUnitId(), "Base unit"),
            required(command.defaultPresentationId(), "Default presentation"),
            productType(command.productType()),
            productStatus(command.status()),
            command.stockTrackingEnabled() == null || command.stockTrackingEnabled()
        );
    }

    private void validateProductReferences(TenantId tenantId, UUID brandId, UUID categoryId, UUID baseUnitId, UUID defaultPresentationId) {
        if (brandId != null && !repository.existsActiveBrand(tenantId, brandId)) {
            throw new CatalogReferenceNotFoundException("brand", brandId);
        }
        if (categoryId != null && !repository.existsActiveCategory(tenantId, categoryId)) {
            throw new CatalogReferenceNotFoundException("category", categoryId);
        }
        ensureActiveUnit(tenantId, baseUnitId);
        if (!repository.existsActivePresentation(tenantId, defaultPresentationId)) {
            throw new CatalogReferenceNotFoundException("presentation", defaultPresentationId);
        }
    }

    private void ensureActiveUnit(TenantId tenantId, UUID unitId) {
        if (!repository.existsActiveUnit(tenantId, unitId)) {
            throw new CatalogReferenceNotFoundException("unit", unitId);
        }
    }

    private void ensureExistingBrand(TenantId tenantId, UUID id) {
        repository.findBrandById(tenantId, id).orElseThrow(() -> notFound("brand"));
    }

    private void ensureExistingCategory(TenantId tenantId, UUID id) {
        repository.findCategoryById(tenantId, id).orElseThrow(() -> notFound("category"));
    }

    private void ensureExistingUnit(TenantId tenantId, UUID id) {
        repository.findUnitById(tenantId, id).orElseThrow(() -> notFound("unit"));
    }

    private void ensureExistingPresentation(TenantId tenantId, UUID id) {
        repository.findPresentationById(tenantId, id).orElseThrow(() -> notFound("presentation"));
    }

    private void ensureExistingProduct(TenantId tenantId, UUID id) {
        repository.findProductById(tenantId, id).orElseThrow(() -> notFound("product"));
    }

    private void audit(TenantContext context, String action, String resourceType, UUID resourceId, CatalogRequestMetadata metadata, Map<String, Object> details) {
        repository.recordAuditEvent(context.tenantId(), context.userId(), action, resourceType, resourceId, "SUCCESS", metadata, details);
    }

    private CatalogItemNotFoundException notFound(String resource) {
        return new CatalogItemNotFoundException(resource);
    }

    private CatalogVersionConflictException conflict(String resource) {
        return new CatalogVersionConflictException(resource);
    }

    private void requireVersion(long version) {
        if (version < 0) {
            throw new IllegalArgumentException("Version is required");
        }
    }

    private String normalizeCode(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    private String required(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private UUID required(UUID value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean active(Boolean value) {
        return value == null || value;
    }

    private int precision(Integer value) {
        int precision = value == null ? 0 : value;
        if (precision < 0 || precision > 6) {
            throw new IllegalArgumentException("Decimal precision must be between 0 and 6");
        }
        return precision;
    }

    private BigDecimal positive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return value;
    }

    private String productType(String value) {
        String normalized = value == null ? "GOOD" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("GOOD") && !normalized.equals("SERVICE")) {
            throw new IllegalArgumentException("Product type must be GOOD or SERVICE");
        }
        return normalized;
    }

    private String productStatus(String value) {
        String normalized = value == null ? "ACTIVE" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("ACTIVE") && !normalized.equals("INACTIVE")) {
            throw new IllegalArgumentException("Product status must be ACTIVE or INACTIVE");
        }
        return normalized;
    }

}