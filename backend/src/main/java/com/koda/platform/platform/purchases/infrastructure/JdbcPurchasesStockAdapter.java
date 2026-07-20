package com.koda.platform.platform.purchases.infrastructure;

import com.koda.platform.platform.purchases.application.PurchaseOperationRejectedException;
import com.koda.platform.platform.purchases.application.PurchasesRequestMetadata;
import com.koda.platform.platform.purchases.application.PurchasesStockMovement;
import com.koda.platform.platform.purchases.application.PurchasesStockPort;
import com.koda.platform.platform.stock.application.CreateStockMovementCommand;
import com.koda.platform.platform.stock.application.StockBalance;
import com.koda.platform.platform.stock.application.StockMovement;
import com.koda.platform.platform.stock.application.StockRepository;
import com.koda.platform.platform.stock.application.StockRequestMetadata;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "koda.purchases.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcPurchasesStockAdapter implements PurchasesStockPort {

    private static final int QUANTITY_SCALE = 6;

    private final StockRepository stockRepository;

    public JdbcPurchasesStockAdapter(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public PurchasesStockMovement receivePurchaseStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, UUID purchaseId,
                                                       UUID purchaseItemId, PurchasesRequestMetadata metadata) {
        return createMovement(context, warehouseId, productId, quantity, "IN", purchaseId, purchaseItemId, metadata);
    }

    @Override
    public PurchasesStockMovement reversePurchaseStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, UUID purchaseId,
                                                       UUID purchaseItemId, PurchasesRequestMetadata metadata) {
        return createMovement(context, warehouseId, productId, quantity, "OUT", purchaseId, purchaseItemId, metadata);
    }

    private PurchasesStockMovement createMovement(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, String movementType,
                                                  UUID purchaseId, UUID purchaseItemId, PurchasesRequestMetadata metadata) {
        TenantId tenantId = context.tenantId();
        BigDecimal normalizedQuantity = scaleQuantity(quantity);
        StockBalance balance = stockRepository.lockOrCreateBalance(tenantId, warehouseId, productId);
        BigDecimal before = scaleQuantity(balance.quantityOnHand());
        BigDecimal after = movementType.equals("IN") ? before.add(normalizedQuantity) : before.subtract(normalizedQuantity);
        if (after.signum() < 0) {
            throw new PurchaseOperationRejectedException("INSUFFICIENT_STOCK_TO_REVERSE_PURCHASE", "Purchase cancellation would create negative stock");
        }
        BigDecimal reserved = scaleQuantity(balance.reservedQuantity() == null ? BigDecimal.ZERO : balance.reservedQuantity());
        if (movementType.equals("OUT") && after.compareTo(reserved) < 0) {
            throw new PurchaseOperationRejectedException("RESERVED_STOCK_CONFLICT", "Purchase cancellation would leave reserved stock uncovered");
        }
        after = after.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
        BigDecimal delta = after.subtract(before).setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
        StockBalance updated = stockRepository.updateBalanceQuantity(tenantId, balance.id(), after);
        CreateStockMovementCommand command = new CreateStockMovementCommand(
            warehouseId,
            productId,
            movementType,
            normalizedQuantity,
            null,
            null,
            "PURCHASE",
            purchaseId,
            movementType.equals("IN") ? "Purchase confirmation item " + purchaseItemId : "Purchase cancellation item " + purchaseItemId
        );
        StockMovement movement = stockRepository.createMovement(tenantId, context.userId(), command, before, updated.quantityOnHand(), delta);
        stockRepository.recordAuditEvent(tenantId, context.userId(), "stock.movement.create", "stock_movement", movement.id(), "SUCCESS",
            new StockRequestMetadata(metadata == null ? null : metadata.sourceIp(), metadata == null ? null : metadata.userAgent()),
            details(movement, before, updated.quantityOnHand(), purchaseId, purchaseItemId));
        return new PurchasesStockMovement(movement.id());
    }

    private BigDecimal scaleQuantity(BigDecimal value) {
        return value.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }

    private Map<String, Object> details(StockMovement movement, BigDecimal quantityBefore, BigDecimal quantityAfter, UUID purchaseId, UUID purchaseItemId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("movementType", movement.movementType());
        details.put("warehouseId", movement.warehouseId());
        details.put("productId", movement.productId());
        details.put("quantity", movement.quantity());
        details.put("quantityBefore", quantityBefore);
        details.put("quantityAfter", quantityAfter);
        details.put("quantityDelta", movement.quantityDelta());
        details.put("purchaseId", purchaseId);
        details.put("purchaseItemId", purchaseItemId);
        return details;
    }
}