package com.koda.platform.platform.purchases.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PurchasesRepository {

    List<Purchase> listPurchases(TenantId tenantId, int limit);

    Optional<Purchase> findById(TenantId tenantId, UUID id);

    Optional<String> findTenantCurrency(TenantId tenantId);

    boolean existsActiveBranch(TenantId tenantId, UUID branchId);

    boolean existsActiveWarehouse(TenantId tenantId, UUID branchId, UUID warehouseId);

    Optional<PurchaseSupplier> findActiveSupplier(TenantId tenantId, UUID id);

    Optional<PurchaseProduct> findPurchasableProduct(TenantId tenantId, UUID id);

    Purchase createDraft(TenantId tenantId, UUID actorUserId, PreparedPurchaseDraft draft);

    Optional<Purchase> replaceDraft(TenantId tenantId, UUID id, UUID actorUserId, long version, PreparedPurchaseDraft draft);

    boolean softDeleteDraft(TenantId tenantId, UUID id, UUID actorUserId, long version);

    Optional<Purchase> confirmPurchase(TenantId tenantId, UUID id, UUID actorUserId, long version, List<PurchaseItemStockUpdate> stockUpdates,
                                       PurchasePaymentUpdate paymentUpdate);

    Optional<Purchase> cancelPurchase(TenantId tenantId, UUID id, UUID actorUserId, long version, String reason,
                                      List<PurchaseItemStockReversal> stockReversals, PurchasePaymentReversalUpdate paymentReversal);

    void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                          String outcome, PurchasesRequestMetadata metadata, Map<String, Object> details);
}