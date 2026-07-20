package com.koda.platform.platform.sales.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SalesRepository {

    List<Sale> listSales(TenantId tenantId, int limit);

    Optional<Sale> findById(TenantId tenantId, UUID id);

    Optional<String> findTenantCurrency(TenantId tenantId);

    boolean existsActiveBranch(TenantId tenantId, UUID branchId);

    boolean existsActiveWarehouse(TenantId tenantId, UUID branchId, UUID warehouseId);

    Optional<SalesCustomer> findActiveCustomer(TenantId tenantId, UUID id);

    Optional<SalesCustomer> findDefaultCustomer(TenantId tenantId);

    Optional<SalesProduct> findSellableProduct(TenantId tenantId, UUID id);

    Sale createDraft(TenantId tenantId, UUID actorUserId, PreparedSaleDraft draft);

    Optional<Sale> replaceDraft(TenantId tenantId, UUID id, UUID actorUserId, long version, PreparedSaleDraft draft);

    boolean softDeleteDraft(TenantId tenantId, UUID id, UUID actorUserId, long version);

    Optional<Sale> confirmSale(TenantId tenantId, UUID id, UUID actorUserId, long version, List<SaleItemStockUpdate> stockUpdates,
                               SalePaymentUpdate paymentUpdate);

    Optional<Sale> cancelSale(TenantId tenantId, UUID id, UUID actorUserId, long version, String reason,
                              List<SaleItemStockReversal> stockReversals, SalePaymentReversalUpdate paymentReversal);

    void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                          String outcome, SalesRequestMetadata metadata, Map<String, Object> details);
}