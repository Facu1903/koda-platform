package com.koda.platform.platform.catalog.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CatalogRepository {

    List<Brand> listBrands(TenantId tenantId);

    Optional<Brand> findBrandById(TenantId tenantId, UUID id);

    Brand createBrand(TenantId tenantId, UUID actorUserId, CreateBrandCommand command);

    Optional<Brand> updateBrand(TenantId tenantId, UUID id, UUID actorUserId, UpdateBrandCommand command);

    boolean deleteBrand(TenantId tenantId, UUID id, UUID actorUserId, long version);

    boolean existsActiveBrand(TenantId tenantId, UUID id);

    List<Category> listCategories(TenantId tenantId);

    Optional<Category> findCategoryById(TenantId tenantId, UUID id);

    Category createCategory(TenantId tenantId, UUID actorUserId, CreateCategoryCommand command);

    Optional<Category> updateCategory(TenantId tenantId, UUID id, UUID actorUserId, UpdateCategoryCommand command);

    boolean deleteCategory(TenantId tenantId, UUID id, UUID actorUserId, long version);

    boolean existsActiveCategory(TenantId tenantId, UUID id);

    List<UnitOfMeasure> listUnits(TenantId tenantId);

    Optional<UnitOfMeasure> findUnitById(TenantId tenantId, UUID id);

    UnitOfMeasure createUnit(TenantId tenantId, UUID actorUserId, CreateUnitOfMeasureCommand command);

    Optional<UnitOfMeasure> updateUnit(TenantId tenantId, UUID id, UUID actorUserId, UpdateUnitOfMeasureCommand command);

    boolean deleteUnit(TenantId tenantId, UUID id, UUID actorUserId, long version);

    boolean existsActiveUnit(TenantId tenantId, UUID id);

    List<ProductPresentation> listPresentations(TenantId tenantId);

    Optional<ProductPresentation> findPresentationById(TenantId tenantId, UUID id);

    ProductPresentation createPresentation(TenantId tenantId, UUID actorUserId, CreateProductPresentationCommand command);

    Optional<ProductPresentation> updatePresentation(TenantId tenantId, UUID id, UUID actorUserId, UpdateProductPresentationCommand command);

    boolean deletePresentation(TenantId tenantId, UUID id, UUID actorUserId, long version);

    boolean existsActivePresentation(TenantId tenantId, UUID id);

    List<Product> listProducts(TenantId tenantId);

    Optional<Product> findProductById(TenantId tenantId, UUID id);

    Product createProduct(TenantId tenantId, UUID actorUserId, CreateProductCommand command);

    Optional<Product> updateProduct(TenantId tenantId, UUID id, UUID actorUserId, UpdateProductCommand command);

    boolean deleteProduct(TenantId tenantId, UUID id, UUID actorUserId, long version);

    void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                          String outcome, CatalogRequestMetadata metadata, Map<String, Object> details);
}