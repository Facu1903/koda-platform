package com.koda.platform.platform.purchases.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PurchasesServiceTest {

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID userId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID warehouseId = UUID.randomUUID();
    private final UUID cashSessionId = UUID.randomUUID();
    private final UUID supplierId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID serviceProductId = UUID.randomUUID();
    private final FakePurchasesRepository repository = new FakePurchasesRepository();
    private final FakePurchasesStockPort stockPort = new FakePurchasesStockPort();
    private final FakePurchasesCashPort cashPort = new FakePurchasesCashPort();
    private final FakeCurrentTenantProvider currentTenantProvider = new FakeCurrentTenantProvider();
    private final PurchasesRequestMetadata metadata = new PurchasesRequestMetadata("127.0.0.1", "JUnit");
    private PurchasesService service;

    @BeforeEach
    void setUp() {
        service = new PurchasesService(repository, stockPort, cashPort, currentTenantProvider);
        repository.activeBranches.add(branchId);
        repository.activeWarehouses.add(new WarehouseKey(branchId, warehouseId));
        repository.supplier = new PurchaseSupplier(supplierId, "Proveedor", "ACTIVE", false);
        repository.products.put(productId, new PurchaseProduct(productId, "SKU-1", "Producto", "GOOD", "ACTIVE", true, false));
        repository.products.put(serviceProductId, new PurchaseProduct(serviceProductId, "SRV-1", "Servicio", "SERVICE", "ACTIVE", false, false));
        currentTenantProvider.context = Optional.of(context(Set.of("TENANT_ADMIN"), Set.of(
            "purchases:read", "purchases:create", "purchases:update", "purchases:delete", "purchases:confirm", "purchases:cancel",
            "cash_movements:create"
        )));
    }

    @Test
    void createPurchaseRequiresSupplierAndCalculatesTotals() {
        Purchase purchase = service.createPurchase(new CreatePurchaseCommand(branchId, supplierId, " FC-001 ",
            List.of(item(productId, warehouseId, "2", "10.50"))), metadata);

        assertThat(purchase.supplierId()).isEqualTo(supplierId);
        assertThat(purchase.supplierDocumentNumber()).isEqualTo("FC-001");
        assertThat(purchase.status()).isEqualTo("DRAFT");
        assertThat(purchase.numberCode()).isEqualTo("CMP-00000001");
        assertThat(purchase.totalAmount()).isEqualByComparingTo("21.0000");
        assertThat(repository.auditActions).contains("purchases.purchase.create:purchase");
    }

    @Test
    void createPurchaseRejectsMissingSupplier() {
        assertThatThrownBy(() -> service.createPurchase(new CreatePurchaseCommand(branchId, null, null,
            List.of(item(productId, warehouseId, "1", "10"))), metadata))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Supplier is required");
    }

    @Test
    void createPurchaseRejectsServiceProduct() {
        assertThatThrownBy(() -> service.createPurchase(new CreatePurchaseCommand(branchId, supplierId, null,
            List.of(item(serviceProductId, null, "1", "10"))), metadata))
            .isInstanceOf(PurchaseOperationRejectedException.class)
            .satisfies(exception -> assertThat(((PurchaseOperationRejectedException) exception).reasonCode()).isEqualTo("PRODUCT_NOT_PURCHASABLE"));
    }

    @Test
    void createPurchaseRejectsTrackedProductWithoutWarehouse() {
        assertThatThrownBy(() -> service.createPurchase(new CreatePurchaseCommand(branchId, supplierId, null,
            List.of(item(productId, null, "1", "10"))), metadata))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Warehouse is required");
    }

    @Test
    void confirmPurchaseWithPaymentReceivesStockAndPaysCash() {
        Purchase draft = service.createPurchase(new CreatePurchaseCommand(branchId, supplierId, null,
            List.of(item(productId, warehouseId, "1", "15"))), metadata);

        Purchase confirmed = service.confirmPurchase(draft.id(), new ConfirmPurchaseCommand(draft.version(), cashSessionId, "cash"), metadata);

        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(confirmed.paymentStatus()).isEqualTo("PAID");
        assertThat(confirmed.paymentMethod()).isEqualTo("CASH");
        assertThat(confirmed.paidAmount()).isEqualByComparingTo("15.0000");
        assertThat(confirmed.items().getFirst().stockMovementId()).isNotNull();
        assertThat(stockPort.receipts).hasSize(1);
        assertThat(cashPort.payments).hasSize(1);
        assertThat(repository.auditActions).contains("purchases.purchase.confirm:purchase");
    }

    @Test
    void confirmPurchaseWithoutPaymentLeavesPurchaseUnpaid() {
        repository.products.put(productId, new PurchaseProduct(productId, "SKU-1", "Producto", "GOOD", "ACTIVE", false, false));
        Purchase draft = service.createPurchase(new CreatePurchaseCommand(branchId, supplierId, null,
            List.of(item(productId, null, "1", "20"))), metadata);

        Purchase confirmed = service.confirmPurchase(draft.id(), new ConfirmPurchaseCommand(draft.version(), null, null), metadata);

        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(confirmed.paymentStatus()).isEqualTo("UNPAID");
        assertThat(confirmed.cashMovementId()).isNull();
        assertThat(stockPort.receipts).isEmpty();
        assertThat(cashPort.payments).isEmpty();
    }

    @Test
    void cancelPaidPurchaseReversesStockAndCash() {
        Purchase draft = service.createPurchase(new CreatePurchaseCommand(branchId, supplierId, null,
            List.of(item(productId, warehouseId, "1", "15"))), metadata);
        Purchase confirmed = service.confirmPurchase(draft.id(), new ConfirmPurchaseCommand(draft.version(), cashSessionId, "CASH"), metadata);

        Purchase cancelled = service.cancelPurchase(confirmed.id(), new CancelPurchaseCommand(confirmed.version(), "Supplier cancelled", cashSessionId), metadata);

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.paymentStatus()).isEqualTo("REVERSED");
        assertThat(cancelled.items().getFirst().stockReversalMovementId()).isNotNull();
        assertThat(stockPort.reversals).hasSize(1);
        assertThat(cashPort.reversals).hasSize(1);
        assertThat(repository.auditActions).contains("purchases.purchase.cancel:purchase");
    }

    @Test
    void stockUserCanCreateDraftButCannotConfirm() {
        currentTenantProvider.context = Optional.of(context(Set.of("STOCK_USER"), Set.of("purchases:read", "purchases:create", "purchases:update")));
        Purchase draft = service.createPurchase(new CreatePurchaseCommand(branchId, supplierId, null,
            List.of(item(productId, warehouseId, "1", "15"))), metadata);

        assertThatThrownBy(() -> service.confirmPurchase(draft.id(), new ConfirmPurchaseCommand(draft.version(), null, null), metadata))
            .isInstanceOf(PermissionDeniedException.class)
            .satisfies(exception -> assertThat(((PermissionDeniedException) exception).requiredPermission()).isEqualTo("purchases:confirm"));
    }

    @Test
    void paidConfirmationRequiresCashMovementPermission() {
        Purchase draft = service.createPurchase(new CreatePurchaseCommand(branchId, supplierId, null,
            List.of(item(productId, warehouseId, "1", "20"))), metadata);
        currentTenantProvider.context = Optional.of(context(Set.of("MANAGER"), Set.of("purchases:read", "purchases:confirm")));

        assertThatThrownBy(() -> service.confirmPurchase(draft.id(), new ConfirmPurchaseCommand(draft.version(), cashSessionId, "CASH"), metadata))
            .isInstanceOf(PermissionDeniedException.class)
            .satisfies(exception -> assertThat(((PermissionDeniedException) exception).requiredPermission()).isEqualTo("cash_movements:create"));
    }

    private PurchaseItemCommand item(UUID productId, UUID warehouseId, String quantity, String unitCost) {
        return new PurchaseItemCommand(productId, warehouseId, new BigDecimal(quantity), new BigDecimal(unitCost));
    }

    private TenantContext context(Set<String> roles, Set<String> permissions) {
        return new TenantContext(tenantId, userId, roles, permissions, false);
    }

    private final class FakeCurrentTenantProvider implements CurrentTenantProvider {
        private Optional<TenantContext> context = Optional.empty();

        @Override
        public Optional<TenantContext> currentContext() {
            return context;
        }
    }

    private final class FakePurchasesStockPort implements PurchasesStockPort {
        private final List<UUID> receipts = new ArrayList<>();
        private final List<UUID> reversals = new ArrayList<>();

        @Override
        public PurchasesStockMovement receivePurchaseStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity,
                                                           UUID purchaseId, UUID purchaseItemId, PurchasesRequestMetadata metadata) {
            UUID id = UUID.randomUUID();
            receipts.add(id);
            return new PurchasesStockMovement(id);
        }

        @Override
        public PurchasesStockMovement reversePurchaseStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity,
                                                           UUID purchaseId, UUID purchaseItemId, PurchasesRequestMetadata metadata) {
            UUID id = UUID.randomUUID();
            reversals.add(id);
            return new PurchasesStockMovement(id);
        }
    }

    private final class FakePurchasesCashPort implements PurchasesCashPort {
        private final List<UUID> payments = new ArrayList<>();
        private final List<UUID> reversals = new ArrayList<>();

        @Override
        public PurchasesCashMovement recordPurchasePayment(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount,
                                                           String currencyCode, UUID purchaseId, PurchasesRequestMetadata metadata) {
            UUID id = UUID.randomUUID();
            payments.add(id);
            return new PurchasesCashMovement(id);
        }

        @Override
        public PurchasesCashMovement reversePurchasePayment(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount,
                                                            String currencyCode, UUID purchaseId, PurchasesRequestMetadata metadata) {
            UUID id = UUID.randomUUID();
            reversals.add(id);
            return new PurchasesCashMovement(id);
        }
    }

    private final class FakePurchasesRepository implements PurchasesRepository {
        private final List<UUID> activeBranches = new ArrayList<>();
        private final List<WarehouseKey> activeWarehouses = new ArrayList<>();
        private final Map<UUID, PurchaseProduct> products = new HashMap<>();
        private final List<Purchase> purchases = new ArrayList<>();
        private final List<String> auditActions = new ArrayList<>();
        private PurchaseSupplier supplier;
        private long nextNumber = 1;

        @Override
        public List<Purchase> listPurchases(TenantId tenantId, int limit) {
            return purchases.stream().filter(purchase -> purchase.tenantId().equals(tenantId)).limit(limit).toList();
        }

        @Override
        public Optional<Purchase> findById(TenantId tenantId, UUID id) {
            return purchases.stream().filter(purchase -> purchase.tenantId().equals(tenantId) && purchase.id().equals(id)).findFirst();
        }

        @Override
        public Optional<String> findTenantCurrency(TenantId tenantId) {
            return Optional.of("ARS");
        }

        @Override
        public boolean existsActiveBranch(TenantId tenantId, UUID branchId) {
            return activeBranches.contains(branchId);
        }

        @Override
        public boolean existsActiveWarehouse(TenantId tenantId, UUID branchId, UUID warehouseId) {
            return activeWarehouses.contains(new WarehouseKey(branchId, warehouseId));
        }

        @Override
        public Optional<PurchaseSupplier> findActiveSupplier(TenantId tenantId, UUID id) {
            return supplier.id().equals(id) ? Optional.of(supplier) : Optional.empty();
        }

        @Override
        public Optional<PurchaseProduct> findPurchasableProduct(TenantId tenantId, UUID id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public Purchase createDraft(TenantId tenantId, UUID actorUserId, PreparedPurchaseDraft draft) {
            UUID id = UUID.randomUUID();
            long purchaseNumber = nextNumber++;
            Purchase purchase = new Purchase(id, tenantId, draft.branchId(), draft.supplierId(), purchaseNumber, "CMP-%08d".formatted(purchaseNumber),
                draft.supplierDocumentNumber(), "DRAFT", draft.currencyCode(), draft.subtotalAmount(), draft.totalAmount(), "UNPAID", null,
                BigDecimal.ZERO.setScale(4), null, null, null, null, null, null, null, null, null, 0, Instant.parse("2026-07-20T12:00:00Z"),
                toItems(id, draft.items()));
            purchases.add(purchase);
            return purchase;
        }

        @Override
        public Optional<Purchase> replaceDraft(TenantId tenantId, UUID id, UUID actorUserId, long version, PreparedPurchaseDraft draft) {
            Optional<Purchase> existing = findById(tenantId, id).filter(purchase -> purchase.version() == version && purchase.status().equals("DRAFT"));
            if (existing.isEmpty()) {
                return Optional.empty();
            }
            Purchase current = existing.get();
            Purchase updated = new Purchase(current.id(), tenantId, draft.branchId(), draft.supplierId(), current.purchaseNumber(), current.numberCode(),
                draft.supplierDocumentNumber(), "DRAFT", draft.currencyCode(), draft.subtotalAmount(), draft.totalAmount(), "UNPAID", null,
                BigDecimal.ZERO.setScale(4), null, null, null, null, null, null, null, null, null, current.version() + 1,
                Instant.parse("2026-07-20T12:05:00Z"), toItems(id, draft.items()));
            purchases.remove(current);
            purchases.add(updated);
            return Optional.of(updated);
        }

        @Override
        public boolean softDeleteDraft(TenantId tenantId, UUID id, UUID actorUserId, long version) {
            Optional<Purchase> existing = findById(tenantId, id).filter(purchase -> purchase.version() == version && purchase.status().equals("DRAFT"));
            existing.ifPresent(purchases::remove);
            return existing.isPresent();
        }

        @Override
        public Optional<Purchase> confirmPurchase(TenantId tenantId, UUID id, UUID actorUserId, long version,
                                                  List<PurchaseItemStockUpdate> stockUpdates, PurchasePaymentUpdate paymentUpdate) {
            Optional<Purchase> existing = findById(tenantId, id).filter(purchase -> purchase.version() == version && purchase.status().equals("DRAFT"));
            if (existing.isEmpty()) {
                return Optional.empty();
            }
            Purchase current = existing.get();
            Map<UUID, UUID> stockMovementIds = new HashMap<>();
            stockUpdates.forEach(update -> stockMovementIds.put(update.purchaseItemId(), update.stockMovementId()));
            Purchase confirmed = new Purchase(current.id(), current.tenantId(), current.branchId(), current.supplierId(), current.purchaseNumber(),
                current.numberCode(), current.supplierDocumentNumber(), "CONFIRMED", current.currencyCode(), current.subtotalAmount(), current.totalAmount(),
                paymentUpdate == null ? "UNPAID" : "PAID", paymentUpdate == null ? null : paymentUpdate.paymentMethod(),
                paymentUpdate == null ? BigDecimal.ZERO.setScale(4) : paymentUpdate.paidAmount(), paymentUpdate == null ? null : paymentUpdate.cashSessionId(),
                paymentUpdate == null ? null : paymentUpdate.cashMovementId(), null, null, Instant.parse("2026-07-20T12:10:00Z"), actorUserId,
                null, null, null, current.version() + 1, Instant.parse("2026-07-20T12:10:00Z"),
                withStockMovements(current.items(), stockMovementIds, Map.of()));
            purchases.remove(current);
            purchases.add(confirmed);
            return Optional.of(confirmed);
        }

        @Override
        public Optional<Purchase> cancelPurchase(TenantId tenantId, UUID id, UUID actorUserId, long version, String reason,
                                                 List<PurchaseItemStockReversal> stockReversals, PurchasePaymentReversalUpdate paymentReversal) {
            Optional<Purchase> existing = findById(tenantId, id).filter(purchase -> purchase.version() == version && purchase.status().equals("CONFIRMED"));
            if (existing.isEmpty()) {
                return Optional.empty();
            }
            Purchase current = existing.get();
            Map<UUID, UUID> reversals = new HashMap<>();
            stockReversals.forEach(reversal -> reversals.put(reversal.purchaseItemId(), reversal.stockReversalMovementId()));
            Purchase cancelled = new Purchase(current.id(), current.tenantId(), current.branchId(), current.supplierId(), current.purchaseNumber(),
                current.numberCode(), current.supplierDocumentNumber(), "CANCELLED", current.currencyCode(), current.subtotalAmount(), current.totalAmount(),
                paymentReversal == null ? current.paymentStatus() : "REVERSED", current.paymentMethod(), current.paidAmount(), current.cashSessionId(),
                current.cashMovementId(), paymentReversal == null ? null : paymentReversal.cashSessionId(),
                paymentReversal == null ? null : paymentReversal.cashMovementId(), current.confirmedAt(), current.confirmedBy(),
                Instant.parse("2026-07-20T12:20:00Z"), actorUserId, reason, current.version() + 1, Instant.parse("2026-07-20T12:20:00Z"),
                withStockMovements(current.items(), Map.of(), reversals));
            purchases.remove(current);
            purchases.add(cancelled);
            return Optional.of(cancelled);
        }

        @Override
        public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId, String outcome,
                                     PurchasesRequestMetadata metadata, Map<String, Object> details) {
            auditActions.add(action + ":" + resourceType);
        }

        private List<PurchaseItem> toItems(UUID purchaseId, List<PreparedPurchaseItem> items) {
            return items.stream()
                .map(item -> new PurchaseItem(UUID.randomUUID(), purchaseId, item.lineNumber(), item.productId(), item.warehouseId(), item.productSku(),
                    item.productName(), item.productType(), item.stockTrackingEnabled(), item.quantity(), item.unitCost(), item.subtotalAmount(), null, null))
                .toList();
        }

        private List<PurchaseItem> withStockMovements(List<PurchaseItem> items, Map<UUID, UUID> stockMovementIds,
                                                      Map<UUID, UUID> stockReversalMovementIds) {
            return items.stream()
                .map(item -> new PurchaseItem(item.id(), item.purchaseId(), item.lineNumber(), item.productId(), item.warehouseId(), item.productSku(),
                    item.productName(), item.productType(), item.stockTrackingEnabled(), item.quantity(), item.unitCost(), item.subtotalAmount(),
                    stockMovementIds.getOrDefault(item.id(), item.stockMovementId()),
                    stockReversalMovementIds.getOrDefault(item.id(), item.stockReversalMovementId())))
                .toList();
        }
    }

    private record WarehouseKey(UUID branchId, UUID warehouseId) {
    }
}