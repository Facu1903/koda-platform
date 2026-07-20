package com.koda.platform.platform.sales.infrastructure;

import com.koda.platform.platform.sales.application.SaleOperationRejectedException;
import com.koda.platform.platform.sales.application.SalesRequestMetadata;
import com.koda.platform.platform.sales.application.SalesStockMovement;
import com.koda.platform.platform.sales.application.SalesStockPort;
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
@ConditionalOnProperty(prefix = "koda.sales.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSalesStockAdapter implements SalesStockPort {

    private static final int QUANTITY_SCALE = 6;

    private final StockRepository stockRepository;

    public JdbcSalesStockAdapter(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public SalesStockMovement issueSaleStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, UUID saleId, UUID saleItemId,
                                             SalesRequestMetadata metadata) {
        return createMovement(context, warehouseId, productId, quantity, "OUT", saleId, saleItemId, metadata);
    }

    @Override
    public SalesStockMovement reverseSaleStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, UUID saleId, UUID saleItemId,
                                               SalesRequestMetadata metadata) {
        return createMovement(context, warehouseId, productId, quantity, "IN", saleId, saleItemId, metadata);
    }

    private SalesStockMovement createMovement(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, String movementType,
                                              UUID saleId, UUID saleItemId, SalesRequestMetadata metadata) {
        TenantId tenantId = context.tenantId();
        BigDecimal normalizedQuantity = scaleQuantity(quantity);
        StockBalance balance = stockRepository.lockOrCreateBalance(tenantId, warehouseId, productId);
        BigDecimal before = scaleQuantity(balance.quantityOnHand());
        BigDecimal after = movementType.equals("OUT") ? before.subtract(normalizedQuantity) : before.add(normalizedQuantity);
        if (after.signum() < 0) {
            throw new SaleOperationRejectedException("INSUFFICIENT_STOCK", "Sale would create negative stock");
        }
        BigDecimal reserved = scaleQuantity(balance.reservedQuantity() == null ? BigDecimal.ZERO : balance.reservedQuantity());
        if (movementType.equals("OUT") && after.compareTo(reserved) < 0) {
            throw new SaleOperationRejectedException("RESERVED_STOCK_CONFLICT", "Sale would leave reserved stock uncovered");
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
            "SALE",
            saleId,
            movementType.equals("OUT") ? "Sale confirmation item " + saleItemId : "Sale cancellation item " + saleItemId
        );
        StockMovement movement = stockRepository.createMovement(tenantId, context.userId(), command, before, updated.quantityOnHand(), delta);
        stockRepository.recordAuditEvent(tenantId, context.userId(), "stock.movement.create", "stock_movement", movement.id(), "SUCCESS",
            new StockRequestMetadata(metadata == null ? null : metadata.sourceIp(), metadata == null ? null : metadata.userAgent()),
            details(movement, before, updated.quantityOnHand(), saleId, saleItemId));
        return new SalesStockMovement(movement.id());
    }

    private BigDecimal scaleQuantity(BigDecimal value) {
        return value.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }

    private Map<String, Object> details(StockMovement movement, BigDecimal quantityBefore, BigDecimal quantityAfter, UUID saleId, UUID saleItemId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("movementType", movement.movementType());
        details.put("warehouseId", movement.warehouseId());
        details.put("productId", movement.productId());
        details.put("quantity", movement.quantity());
        details.put("quantityBefore", quantityBefore);
        details.put("quantityAfter", quantityAfter);
        details.put("quantityDelta", movement.quantityDelta());
        details.put("saleId", saleId);
        details.put("saleItemId", saleItemId);
        return details;
    }
}