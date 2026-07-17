package com.koda.platform.platform.stock.application;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private static final String BALANCES_READ_PERMISSION = "stock_balances:read";
    private static final String MOVEMENTS_READ_PERMISSION = "stock_movements:read";
    private static final String MOVEMENTS_CREATE_PERMISSION = "stock_movements:create";
    private static final String IN = "IN";
    private static final String OUT = "OUT";
    private static final String ADJUSTMENT = "ADJUSTMENT";
    private static final int QUANTITY_SCALE = 6;
    private static final int COST_SCALE = 4;

    private final StockRepository repository;
    private final CurrentTenantProvider currentTenantProvider;

    public StockService(StockRepository repository, CurrentTenantProvider currentTenantProvider) {
        this.repository = repository;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Transactional(readOnly = true)
    public List<StockBalance> listBalances(UUID warehouseId, UUID productId, int limit) {
        TenantContext context = requirePermission(BALANCES_READ_PERMISSION);
        return repository.listBalances(context.tenantId(), warehouseId, productId, normalizeLimit(limit, 500));
    }

    @Transactional(readOnly = true)
    public List<StockMovement> listMovements(UUID warehouseId, UUID productId, int limit) {
        TenantContext context = requirePermission(MOVEMENTS_READ_PERMISSION);
        return repository.listMovements(context.tenantId(), warehouseId, productId, normalizeLimit(limit, 500));
    }

    @Transactional(readOnly = true)
    public StockMovement getMovement(UUID id) {
        TenantContext context = requirePermission(MOVEMENTS_READ_PERMISSION);
        return repository.findMovementById(context.tenantId(), id)
            .orElseThrow(() -> new StockItemNotFoundException("stock_movement"));
    }

    @Transactional
    public StockMovement createMovement(CreateStockMovementCommand command, StockRequestMetadata metadata) {
        TenantContext context = requirePermission(MOVEMENTS_CREATE_PERMISSION);
        CreateStockMovementCommand normalized = normalizeAndValidate(command);
        TenantId tenantId = context.tenantId();
        validateWarehouse(tenantId, normalized.warehouseId());
        validateProduct(tenantId, normalized.productId());

        StockBalance balance = repository.lockOrCreateBalance(tenantId, normalized.warehouseId(), normalized.productId());
        BigDecimal before = scaleQuantity(balance.quantityOnHand(), "Quantity before");
        BigDecimal after = calculateQuantityAfter(normalized.movementType(), before, normalized.quantity());
        validateAvailableStock(normalized.movementType(), after, balance.reservedQuantity());
        BigDecimal delta = after.subtract(before).setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);

        StockBalance updatedBalance = repository.updateBalanceQuantity(tenantId, balance.id(), after);
        StockMovement movement = repository.createMovement(tenantId, context.userId(), normalized, before, updatedBalance.quantityOnHand(), delta);
        repository.recordAuditEvent(tenantId, context.userId(), "stock.movement.create", "stock_movement", movement.id(), "SUCCESS", metadata,
            auditDetails(movement, balance, updatedBalance));
        return movement;
    }

    private TenantContext requirePermission(String permission) {
        TenantContext context = currentTenantProvider.requireContext();
        if (context.platformAdmin() || context.hasPermission(permission)) {
            return context;
        }
        throw new PermissionDeniedException(permission);
    }

    private CreateStockMovementCommand normalizeAndValidate(CreateStockMovementCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Stock movement request is required");
        }
        UUID warehouseId = required(command.warehouseId(), "Warehouse");
        UUID productId = required(command.productId(), "Product");
        String movementType = required(command.movementType(), "Movement type").toUpperCase(Locale.ROOT);
        if (!movementType.equals(IN) && !movementType.equals(OUT) && !movementType.equals(ADJUSTMENT)) {
            throw new IllegalArgumentException("Movement type must be IN, OUT or ADJUSTMENT");
        }
        BigDecimal quantity = scaleQuantity(required(command.quantity(), "Quantity"), "Quantity");
        if ((movementType.equals(IN) || movementType.equals(OUT)) && quantity.signum() <= 0) {
            throw new IllegalArgumentException("IN and OUT movements require quantity greater than zero");
        }
        if (movementType.equals(ADJUSTMENT) && quantity.signum() < 0) {
            throw new IllegalArgumentException("ADJUSTMENT quantity cannot be negative");
        }

        BigDecimal unitCost = command.unitCost() == null ? null : scaleCost(command.unitCost());
        String currencyCode = trimToNull(command.currencyCode());
        if (currencyCode != null) {
            currencyCode = currencyCode.toUpperCase(Locale.ROOT);
            if (!currencyCode.matches("^[A-Z]{3}$")) {
                throw new IllegalArgumentException("Currency code must use ISO 4217 format");
            }
        }
        if (unitCost != null && currencyCode == null) {
            throw new IllegalArgumentException("Currency code is required when unit cost is present");
        }
        if (unitCost == null && currencyCode != null) {
            throw new IllegalArgumentException("Unit cost is required when currency code is present");
        }

        String referenceType = trimToNull(command.referenceType());
        UUID referenceId = command.referenceId();
        if ((referenceType == null && referenceId != null) || (referenceType != null && referenceId == null)) {
            throw new IllegalArgumentException("Reference type and reference id must be provided together");
        }
        if (referenceType != null) {
            referenceType = referenceType.toUpperCase(Locale.ROOT);
        }

        return new CreateStockMovementCommand(
            warehouseId,
            productId,
            movementType,
            quantity,
            unitCost,
            currencyCode,
            referenceType,
            referenceId,
            trimToNull(command.reason())
        );
    }

    private void validateWarehouse(TenantId tenantId, UUID warehouseId) {
        if (!repository.existsActiveWarehouse(tenantId, warehouseId)) {
            throw new StockReferenceNotFoundException("warehouse", warehouseId);
        }
    }

    private void validateProduct(TenantId tenantId, UUID productId) {
        StockProduct product = repository.findProductForStock(tenantId, productId)
            .orElseThrow(() -> new StockReferenceNotFoundException("product", productId));
        if (!"GOOD".equals(product.productType()) || !"ACTIVE".equals(product.status()) || !product.stockTrackingEnabled()) {
            throw new StockMovementRejectedException("PRODUCT_NOT_STOCKABLE", "Product is not eligible for stock movements");
        }
        if (product.allowNegativeStock()) {
            throw new StockMovementRejectedException("UNSUPPORTED_NEGATIVE_STOCK_POLICY", "Negative stock policy is not supported in Sprint 1");
        }
    }

    private BigDecimal calculateQuantityAfter(String movementType, BigDecimal before, BigDecimal quantity) {
        BigDecimal after;
        if (movementType.equals(IN)) {
            after = before.add(quantity);
        } else if (movementType.equals(OUT)) {
            after = before.subtract(quantity);
        } else {
            after = quantity;
        }
        if (after.signum() < 0) {
            throw new StockMovementRejectedException("INSUFFICIENT_STOCK", "Stock movement would create negative stock");
        }
        return after.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }

    private void validateAvailableStock(String movementType, BigDecimal after, BigDecimal reservedQuantity) {
        BigDecimal reserved = scaleQuantity(reservedQuantity == null ? BigDecimal.ZERO : reservedQuantity, "Reserved quantity");
        if ((movementType.equals(OUT) || movementType.equals(ADJUSTMENT)) && after.compareTo(reserved) < 0) {
            throw new StockMovementRejectedException("RESERVED_STOCK_CONFLICT", "Stock movement would leave reserved stock uncovered");
        }
    }

    private int normalizeLimit(int limit, int max) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        return Math.min(limit, max);
    }

    private BigDecimal scaleQuantity(BigDecimal value, String fieldName) {
        BigDecimal stripped = value.stripTrailingZeros();
        int effectiveScale = Math.max(0, stripped.scale());
        if (effectiveScale > QUANTITY_SCALE) {
            throw new IllegalArgumentException(fieldName + " supports up to 6 decimals");
        }
        return value.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }

    private BigDecimal scaleCost(BigDecimal value) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Unit cost cannot be negative");
        }
        BigDecimal stripped = value.stripTrailingZeros();
        int effectiveScale = Math.max(0, stripped.scale());
        if (effectiveScale > COST_SCALE) {
            throw new IllegalArgumentException("Unit cost supports up to 4 decimals");
        }
        return value.setScale(COST_SCALE, RoundingMode.UNNECESSARY);
    }

    private <T> T required(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private String required(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> auditDetails(StockMovement movement, StockBalance before, StockBalance after) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("movementType", movement.movementType());
        details.put("warehouseId", movement.warehouseId());
        details.put("productId", movement.productId());
        details.put("quantity", movement.quantity());
        details.put("quantityBefore", movement.quantityBefore());
        details.put("quantityAfter", movement.quantityAfter());
        details.put("quantityDelta", movement.quantityDelta());
        details.put("balanceVersionBefore", before.version());
        details.put("balanceVersionAfter", after.version());
        return details;
    }
}