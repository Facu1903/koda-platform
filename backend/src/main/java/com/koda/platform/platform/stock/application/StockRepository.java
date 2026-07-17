package com.koda.platform.platform.stock.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository {

    List<StockBalance> listBalances(TenantId tenantId, UUID warehouseId, UUID productId, int limit);

    List<StockMovement> listMovements(TenantId tenantId, UUID warehouseId, UUID productId, int limit);

    Optional<StockMovement> findMovementById(TenantId tenantId, UUID id);

    Optional<StockProduct> findProductForStock(TenantId tenantId, UUID id);

    boolean existsActiveWarehouse(TenantId tenantId, UUID id);

    StockBalance lockOrCreateBalance(TenantId tenantId, UUID warehouseId, UUID productId);

    StockBalance updateBalanceQuantity(TenantId tenantId, UUID balanceId, BigDecimal quantityOnHand);

    StockMovement createMovement(TenantId tenantId, UUID actorUserId, CreateStockMovementCommand command,
                                 BigDecimal quantityBefore, BigDecimal quantityAfter, BigDecimal quantityDelta);

    void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                          String outcome, StockRequestMetadata metadata, Map<String, Object> details);
}