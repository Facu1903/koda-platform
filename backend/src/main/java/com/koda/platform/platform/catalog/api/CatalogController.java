package com.koda.platform.platform.catalog.api;

import com.koda.platform.platform.catalog.application.Brand;
import com.koda.platform.platform.catalog.application.CatalogRequestMetadata;
import com.koda.platform.platform.catalog.application.CatalogService;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/brands")
    public List<BrandResponse> listBrands() {
        return catalogService.listBrands().stream().map(BrandResponse::from).toList();
    }

    @GetMapping("/brands/{id}")
    public BrandResponse getBrand(@PathVariable UUID id) {
        return BrandResponse.from(catalogService.getBrand(id));
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public BrandResponse createBrand(@Valid @RequestBody BrandRequest request, HttpServletRequest httpRequest) {
        return BrandResponse.from(catalogService.createBrand(request.toCreateCommand(), metadata(httpRequest)));
    }

    @PutMapping("/brands/{id}")
    public BrandResponse updateBrand(@PathVariable UUID id, @Valid @RequestBody VersionedBrandRequest request, HttpServletRequest httpRequest) {
        return BrandResponse.from(catalogService.updateBrand(id, request.toUpdateCommand(), metadata(httpRequest)));
    }

    @DeleteMapping("/brands/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrand(@PathVariable UUID id, @RequestParam @Min(0) long version, HttpServletRequest httpRequest) {
        catalogService.deleteBrand(id, version, metadata(httpRequest));
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return catalogService.listCategories().stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/categories/{id}")
    public CategoryResponse getCategory(@PathVariable UUID id) {
        return CategoryResponse.from(catalogService.getCategory(id));
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request, HttpServletRequest httpRequest) {
        return CategoryResponse.from(catalogService.createCategory(request.toCreateCommand(), metadata(httpRequest)));
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable UUID id, @Valid @RequestBody VersionedCategoryRequest request, HttpServletRequest httpRequest) {
        return CategoryResponse.from(catalogService.updateCategory(id, request.toUpdateCommand(), metadata(httpRequest)));
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID id, @RequestParam @Min(0) long version, HttpServletRequest httpRequest) {
        catalogService.deleteCategory(id, version, metadata(httpRequest));
    }

    @GetMapping("/units")
    public List<UnitResponse> listUnits() {
        return catalogService.listUnits().stream().map(UnitResponse::from).toList();
    }

    @GetMapping("/units/{id}")
    public UnitResponse getUnit(@PathVariable UUID id) {
        return UnitResponse.from(catalogService.getUnit(id));
    }

    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    public UnitResponse createUnit(@Valid @RequestBody UnitRequest request, HttpServletRequest httpRequest) {
        return UnitResponse.from(catalogService.createUnit(request.toCreateCommand(), metadata(httpRequest)));
    }

    @PutMapping("/units/{id}")
    public UnitResponse updateUnit(@PathVariable UUID id, @Valid @RequestBody VersionedUnitRequest request, HttpServletRequest httpRequest) {
        return UnitResponse.from(catalogService.updateUnit(id, request.toUpdateCommand(), metadata(httpRequest)));
    }

    @DeleteMapping("/units/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUnit(@PathVariable UUID id, @RequestParam @Min(0) long version, HttpServletRequest httpRequest) {
        catalogService.deleteUnit(id, version, metadata(httpRequest));
    }

    @GetMapping("/presentations")
    public List<PresentationResponse> listPresentations() {
        return catalogService.listPresentations().stream().map(PresentationResponse::from).toList();
    }

    @GetMapping("/presentations/{id}")
    public PresentationResponse getPresentation(@PathVariable UUID id) {
        return PresentationResponse.from(catalogService.getPresentation(id));
    }

    @PostMapping("/presentations")
    @ResponseStatus(HttpStatus.CREATED)
    public PresentationResponse createPresentation(@Valid @RequestBody PresentationRequest request, HttpServletRequest httpRequest) {
        return PresentationResponse.from(catalogService.createPresentation(request.toCreateCommand(), metadata(httpRequest)));
    }

    @PutMapping("/presentations/{id}")
    public PresentationResponse updatePresentation(@PathVariable UUID id, @Valid @RequestBody VersionedPresentationRequest request,
                                                   HttpServletRequest httpRequest) {
        return PresentationResponse.from(catalogService.updatePresentation(id, request.toUpdateCommand(), metadata(httpRequest)));
    }

    @DeleteMapping("/presentations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePresentation(@PathVariable UUID id, @RequestParam @Min(0) long version, HttpServletRequest httpRequest) {
        catalogService.deletePresentation(id, version, metadata(httpRequest));
    }

    @GetMapping("/products")
    public List<ProductResponse> listProducts() {
        return catalogService.listProducts().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/products/{id}")
    public ProductResponse getProduct(@PathVariable UUID id) {
        return ProductResponse.from(catalogService.getProduct(id));
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request, HttpServletRequest httpRequest) {
        return ProductResponse.from(catalogService.createProduct(request.toCreateCommand(), metadata(httpRequest)));
    }

    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody VersionedProductRequest request, HttpServletRequest httpRequest) {
        return ProductResponse.from(catalogService.updateProduct(id, request.toUpdateCommand(), metadata(httpRequest)));
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id, @RequestParam @Min(0) long version, HttpServletRequest httpRequest) {
        catalogService.deleteProduct(id, version, metadata(httpRequest));
    }

    private CatalogRequestMetadata metadata(HttpServletRequest request) {
        return new CatalogRequestMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public record BrandRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        Boolean active
    ) {
        CreateBrandCommand toCreateCommand() {
            return new CreateBrandCommand(code, name, description, active);
        }
    }

    public record VersionedBrandRequest(
        @Min(0) long version,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        @NotNull Boolean active
    ) {
        UpdateBrandCommand toUpdateCommand() {
            return new UpdateBrandCommand(version, code, name, description, active);
        }
    }

    public record CategoryRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        Boolean active
    ) {
        CreateCategoryCommand toCreateCommand() {
            return new CreateCategoryCommand(code, name, description, active);
        }
    }

    public record VersionedCategoryRequest(
        @Min(0) long version,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        @NotNull Boolean active
    ) {
        UpdateCategoryCommand toUpdateCommand() {
            return new UpdateCategoryCommand(version, code, name, description, active);
        }
    }

    public record UnitRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 24) String symbol,
        @Min(0) @Max(6) Integer decimalPrecision,
        Boolean active
    ) {
        CreateUnitOfMeasureCommand toCreateCommand() {
            return new CreateUnitOfMeasureCommand(code, name, symbol, decimalPrecision, active);
        }
    }

    public record VersionedUnitRequest(
        @Min(0) long version,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 24) String symbol,
        @Min(0) @Max(6) @NotNull Integer decimalPrecision,
        @NotNull Boolean active
    ) {
        UpdateUnitOfMeasureCommand toUpdateCommand() {
            return new UpdateUnitOfMeasureCommand(version, code, name, symbol, decimalPrecision, active);
        }
    }

    public record PresentationRequest(
        @NotNull UUID unitId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 160) String name,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
        Boolean active
    ) {
        CreateProductPresentationCommand toCreateCommand() {
            return new CreateProductPresentationCommand(unitId, code, name, quantity, active);
        }
    }

    public record VersionedPresentationRequest(
        @Min(0) long version,
        @NotNull UUID unitId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 160) String name,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
        @NotNull Boolean active
    ) {
        UpdateProductPresentationCommand toUpdateCommand() {
            return new UpdateProductPresentationCommand(version, unitId, code, name, quantity, active);
        }
    }

    public record ProductRequest(
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 220) String name,
        @Size(max = 1000) String description,
        @Size(max = 80) String barcode,
        UUID brandId,
        UUID categoryId,
        @NotNull UUID baseUnitId,
        @NotNull UUID defaultPresentationId,
        @Size(max = 32) String productType,
        @Size(max = 32) String status,
        Boolean stockTrackingEnabled
    ) {
        CreateProductCommand toCreateCommand() {
            return new CreateProductCommand(sku, name, description, barcode, brandId, categoryId, baseUnitId, defaultPresentationId,
                productType, status, stockTrackingEnabled);
        }
    }

    public record VersionedProductRequest(
        @Min(0) long version,
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 220) String name,
        @Size(max = 1000) String description,
        @Size(max = 80) String barcode,
        UUID brandId,
        UUID categoryId,
        @NotNull UUID baseUnitId,
        @NotNull UUID defaultPresentationId,
        @Size(max = 32) String productType,
        @Size(max = 32) String status,
        @NotNull Boolean stockTrackingEnabled
    ) {
        UpdateProductCommand toUpdateCommand() {
            return new UpdateProductCommand(version, sku, name, description, barcode, brandId, categoryId, baseUnitId, defaultPresentationId,
                productType, status, stockTrackingEnabled);
        }
    }

    public record BrandResponse(String id, String code, String name, String description, boolean active, long version, Instant updatedAt) {
        static BrandResponse from(Brand brand) {
            return new BrandResponse(brand.id().toString(), brand.code(), brand.name(), brand.description(), brand.active(), brand.version(), brand.updatedAt());
        }
    }

    public record CategoryResponse(String id, String code, String name, String description, boolean active, long version, Instant updatedAt) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.id().toString(), category.code(), category.name(), category.description(), category.active(), category.version(), category.updatedAt());
        }
    }

    public record UnitResponse(String id, String code, String name, String symbol, int decimalPrecision, boolean active, long version, Instant updatedAt) {
        static UnitResponse from(UnitOfMeasure unit) {
            return new UnitResponse(unit.id().toString(), unit.code(), unit.name(), unit.symbol(), unit.decimalPrecision(), unit.active(), unit.version(), unit.updatedAt());
        }
    }

    public record PresentationResponse(String id, String unitId, String code, String name, BigDecimal quantity, boolean active, long version,
                                       Instant updatedAt) {
        static PresentationResponse from(ProductPresentation presentation) {
            return new PresentationResponse(presentation.id().toString(), presentation.unitId().toString(), presentation.code(), presentation.name(),
                presentation.quantity(), presentation.active(), presentation.version(), presentation.updatedAt());
        }
    }

    public record ProductResponse(
        String id,
        String sku,
        String name,
        String description,
        String barcode,
        String brandId,
        String categoryId,
        String baseUnitId,
        String defaultPresentationId,
        String productType,
        String status,
        boolean stockTrackingEnabled,
        boolean allowNegativeStock,
        long version,
        Instant updatedAt
    ) {
        static ProductResponse from(Product product) {
            return new ProductResponse(
                product.id().toString(),
                product.sku(),
                product.name(),
                product.description(),
                product.barcode(),
                product.brandId() == null ? null : product.brandId().toString(),
                product.categoryId() == null ? null : product.categoryId().toString(),
                product.baseUnitId().toString(),
                product.defaultPresentationId().toString(),
                product.productType(),
                product.status(),
                product.stockTrackingEnabled(),
                product.allowNegativeStock(),
                product.version(),
                product.updatedAt()
            );
        }
    }
}