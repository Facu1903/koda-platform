package com.koda.platform.platform.sales.application;

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

class SalesServiceTest {

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID userId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID warehouseId = UUID.randomUUID();
    private final UUID cashSessionId = UUID.randomUUID();
    private final UUID defaultCustomerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID serviceProductId = UUID.randomUUID();
    private final FakeSalesRepository repository = new FakeSalesRepository();
    private final FakeSalesStockPort stockPort = new FakeSalesStockPort();
    private final FakeSalesCashPort cashPort = new FakeSalesCashPort();
    private final FakeCurrentTenantProvider currentTenantProvider = new FakeCurrentTenantProvider();
    private final SalesRequestMetadata metadata = new SalesRequestMetadata("127.0.0.1", "JUnit");
    private SalesService service;

    @BeforeEach
    void setUp() {
        service = new SalesService(repository, stockPort, cashPort, currentTenantProvider);
        repository.activeBranches.add(branchId);
        repository.activeWarehouses.add(new WarehouseKey(branchId, warehouseId));
        repository.defaultCustomer = new SalesCustomer(defaultCustomerId, "Consumidor Final", "ACTIVE", true);
        repository.products.put(productId, new SalesProduct(productId, "SKU-1", "Producto", "GOOD", "ACTIVE", true, false));
        repository.products.put(serviceProductId, new SalesProduct(serviceProductId, "SRV-1", "Servicio", "SERVICE", "ACTIVE", false, false));
        currentTenantProvider.context = Optional.of(context(Set.of("TENANT_ADMIN"), Set.of(
            "sales:read", "sales:create", "sales:update", "sales:delete", "sales:confirm", "sales:cancel", "cash_movements:create"
        )));
    }

    @Test
    void createSaleDefaultsCustomerAndCalculatesTotals() {
        Sale sale = service.createSale(new CreateSaleCommand(branchId, null, List.of(item(productId, warehouseId, "2", "10.50"))), metadata);

        assertThat(sale.customerId()).isEqualTo(defaultCustomerId);
        assertThat(sale.status()).isEqualTo("DRAFT");
        assertThat(sale.numberCode()).isEqualTo("VTA-00000001");
        assertThat(sale.totalAmount()).isEqualByComparingTo("21.0000");
        assertThat(sale.items()).hasSize(1);
        assertThat(repository.auditActions).contains("sales.sale.create:sale");
    }

    @Test
    void createSaleRejectsTrackedProductWithoutWarehouse() {
        assertThatThrownBy(() -> service.createSale(new CreateSaleCommand(branchId, null, List.of(item(productId, null, "1", "10"))), metadata))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Warehouse is required");
    }

    @Test
    void confirmSaleWithPaymentIssuesStockAndCash() {
        Sale draft = service.createSale(new CreateSaleCommand(branchId, null, List.of(item(productId, warehouseId, "1", "15"))), metadata);

        Sale confirmed = service.confirmSale(draft.id(), new ConfirmSaleCommand(draft.version(), cashSessionId, "cash"), metadata);

        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(confirmed.paymentStatus()).isEqualTo("PAID");
        assertThat(confirmed.paymentMethod()).isEqualTo("CASH");
        assertThat(confirmed.paidAmount()).isEqualByComparingTo("15.0000");
        assertThat(confirmed.items().getFirst().stockMovementId()).isNotNull();
        assertThat(stockPort.issues).hasSize(1);
        assertThat(cashPort.payments).hasSize(1);
        assertThat(repository.auditActions).contains("sales.sale.confirm:sale");
    }

    @Test
    void confirmSaleWithoutPaymentLeavesSaleUnpaidAndDoesNotTouchCash() {
        Sale draft = service.createSale(new CreateSaleCommand(branchId, null, List.of(item(serviceProductId, null, "1", "20"))), metadata);

        Sale confirmed = service.confirmSale(draft.id(), new ConfirmSaleCommand(draft.version(), null, null), metadata);

        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(confirmed.paymentStatus()).isEqualTo("UNPAID");
        assertThat(confirmed.cashMovementId()).isNull();
        assertThat(stockPort.issues).isEmpty();
        assertThat(cashPort.payments).isEmpty();
    }

    @Test
    void cancelPaidSaleReversesStockAndCash() {
        Sale draft = service.createSale(new CreateSaleCommand(branchId, null, List.of(item(productId, warehouseId, "1", "15"))), metadata);
        Sale confirmed = service.confirmSale(draft.id(), new ConfirmSaleCommand(draft.version(), cashSessionId, "CASH"), metadata);

        Sale cancelled = service.cancelSale(confirmed.id(), new CancelSaleCommand(confirmed.version(), "Customer cancelled", cashSessionId), metadata);

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.paymentStatus()).isEqualTo("REVERSED");
        assertThat(cancelled.items().getFirst().stockReversalMovementId()).isNotNull();
        assertThat(stockPort.reversals).hasSize(1);
        assertThat(cashPort.reversals).hasSize(1);
        assertThat(repository.auditActions).contains("sales.sale.cancel:sale");
    }

    @Test
    void confirmedSaleCannotBeUpdated() {
        Sale draft = service.createSale(new CreateSaleCommand(branchId, null, List.of(item(serviceProductId, null, "1", "20"))), metadata);
        Sale confirmed = service.confirmSale(draft.id(), new ConfirmSaleCommand(draft.version(), null, null), metadata);

        assertThatThrownBy(() -> service.updateSale(confirmed.id(), new UpdateSaleCommand(confirmed.version(), branchId, null,
            List.of(item(serviceProductId, null, "1", "30"))), metadata))
            .isInstanceOf(SaleOperationRejectedException.class)
            .satisfies(exception -> assertThat(((SaleOperationRejectedException) exception).reasonCode()).isEqualTo("SALE_NOT_DRAFT"));
    }

    @Test
    void createSaleRejectsMissingPermission() {
        currentTenantProvider.context = Optional.of(context(Set.of("READ_ONLY"), Set.of("sales:read")));

        assertThatThrownBy(() -> service.createSale(new CreateSaleCommand(branchId, null, List.of(item(serviceProductId, null, "1", "20"))), metadata))
            .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void paidConfirmationRequiresCashMovementPermission() {
        Sale draft = service.createSale(new CreateSaleCommand(branchId, null, List.of(item(serviceProductId, null, "1", "20"))), metadata);
        currentTenantProvider.context = Optional.of(context(Set.of("SALES_USER"), Set.of("sales:read", "sales:confirm")));

        assertThatThrownBy(() -> service.confirmSale(draft.id(), new ConfirmSaleCommand(draft.version(), cashSessionId, "CASH"), metadata))
            .isInstanceOf(PermissionDeniedException.class)
            .satisfies(exception -> assertThat(((PermissionDeniedException) exception).requiredPermission()).isEqualTo("cash_movements:create"));
    }

    private SaleItemCommand item(UUID productId, UUID warehouseId, String quantity, String unitPrice) {
        return new SaleItemCommand(productId, warehouseId, new BigDecimal(quantity), new BigDecimal(unitPrice));
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

    private final class FakeSalesStockPort implements SalesStockPort {
        private final List<UUID> issues = new ArrayList<>();
        private final List<UUID> reversals = new ArrayList<>();

        @Override
        public SalesStockMovement issueSaleStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, UUID saleId,
                                                 UUID saleItemId, SalesRequestMetadata metadata) {
            UUID id = UUID.randomUUID();
            issues.add(id);
            return new SalesStockMovement(id);
        }

        @Override
        public SalesStockMovement reverseSaleStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, UUID saleId,
                                                   UUID saleItemId, SalesRequestMetadata metadata) {
            UUID id = UUID.randomUUID();
            reversals.add(id);
            return new SalesStockMovement(id);
        }
    }

    private final class FakeSalesCashPort implements SalesCashPort {
        private final List<UUID> payments = new ArrayList<>();
        private final List<UUID> reversals = new ArrayList<>();

        @Override
        public SalesCashMovement recordSalePayment(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount,
                                                   String currencyCode, UUID saleId, SalesRequestMetadata metadata) {
            UUID id = UUID.randomUUID();
            payments.add(id);
            return new SalesCashMovement(id);
        }

        @Override
        public SalesCashMovement reverseSalePayment(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount,
                                                    String currencyCode, UUID saleId, SalesRequestMetadata metadata) {
            UUID id = UUID.randomUUID();
            reversals.add(id);
            return new SalesCashMovement(id);
        }
    }

    private final class FakeSalesRepository implements SalesRepository {
        private final List<UUID> activeBranches = new ArrayList<>();
        private final List<WarehouseKey> activeWarehouses = new ArrayList<>();
        private final Map<UUID, SalesProduct> products = new HashMap<>();
        private final List<Sale> sales = new ArrayList<>();
        private final List<String> auditActions = new ArrayList<>();
        private SalesCustomer defaultCustomer;
        private long nextNumber = 1;

        @Override
        public List<Sale> listSales(TenantId tenantId, int limit) {
            return sales.stream().filter(sale -> sale.tenantId().equals(tenantId)).limit(limit).toList();
        }

        @Override
        public Optional<Sale> findById(TenantId tenantId, UUID id) {
            return sales.stream().filter(sale -> sale.tenantId().equals(tenantId) && sale.id().equals(id)).findFirst();
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
        public Optional<SalesCustomer> findActiveCustomer(TenantId tenantId, UUID id) {
            return defaultCustomer.id().equals(id) ? Optional.of(defaultCustomer) : Optional.empty();
        }

        @Override
        public Optional<SalesCustomer> findDefaultCustomer(TenantId tenantId) {
            return Optional.of(defaultCustomer);
        }

        @Override
        public Optional<SalesProduct> findSellableProduct(TenantId tenantId, UUID id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public Sale createDraft(TenantId tenantId, UUID actorUserId, PreparedSaleDraft draft) {
            UUID id = UUID.randomUUID();
            long saleNumber = nextNumber++;
            Sale sale = new Sale(id, tenantId, draft.branchId(), draft.customerId(), saleNumber, "VTA-%08d".formatted(saleNumber), "DRAFT",
                draft.currencyCode(), draft.subtotalAmount(), draft.totalAmount(), "UNPAID", null, BigDecimal.ZERO.setScale(4), null, null, null,
                null, null, null, null, null, null, 0, Instant.parse("2026-07-20T12:00:00Z"), toItems(id, draft.items(), Map.of(), Map.of()));
            sales.add(sale);
            return sale;
        }

        @Override
        public Optional<Sale> replaceDraft(TenantId tenantId, UUID id, UUID actorUserId, long version, PreparedSaleDraft draft) {
            Optional<Sale> existing = findById(tenantId, id).filter(sale -> sale.version() == version && sale.status().equals("DRAFT"));
            if (existing.isEmpty()) {
                return Optional.empty();
            }
            Sale current = existing.get();
            Sale updated = new Sale(current.id(), tenantId, draft.branchId(), draft.customerId(), current.saleNumber(), current.numberCode(), "DRAFT",
                draft.currencyCode(), draft.subtotalAmount(), draft.totalAmount(), "UNPAID", null, BigDecimal.ZERO.setScale(4), null, null, null,
                null, null, null, null, null, null, current.version() + 1, Instant.parse("2026-07-20T12:05:00Z"),
                toItems(id, draft.items(), Map.of(), Map.of()));
            sales.remove(current);
            sales.add(updated);
            return Optional.of(updated);
        }

        @Override
        public boolean softDeleteDraft(TenantId tenantId, UUID id, UUID actorUserId, long version) {
            Optional<Sale> existing = findById(tenantId, id).filter(sale -> sale.version() == version && sale.status().equals("DRAFT"));
            existing.ifPresent(sales::remove);
            return existing.isPresent();
        }

        @Override
        public Optional<Sale> confirmSale(TenantId tenantId, UUID id, UUID actorUserId, long version, List<SaleItemStockUpdate> stockUpdates,
                                          SalePaymentUpdate paymentUpdate) {
            Optional<Sale> existing = findById(tenantId, id).filter(sale -> sale.version() == version && sale.status().equals("DRAFT"));
            if (existing.isEmpty()) {
                return Optional.empty();
            }
            Sale current = existing.get();
            Map<UUID, UUID> stockMovementIds = new HashMap<>();
            stockUpdates.forEach(update -> stockMovementIds.put(update.saleItemId(), update.stockMovementId()));
            Sale confirmed = new Sale(current.id(), current.tenantId(), current.branchId(), current.customerId(), current.saleNumber(), current.numberCode(),
                "CONFIRMED", current.currencyCode(), current.subtotalAmount(), current.totalAmount(), paymentUpdate == null ? "UNPAID" : "PAID",
                paymentUpdate == null ? null : paymentUpdate.paymentMethod(), paymentUpdate == null ? BigDecimal.ZERO.setScale(4) : paymentUpdate.paidAmount(),
                paymentUpdate == null ? null : paymentUpdate.cashSessionId(), paymentUpdate == null ? null : paymentUpdate.cashMovementId(), null, null,
                Instant.parse("2026-07-20T12:10:00Z"), actorUserId, null, null, null, current.version() + 1,
                Instant.parse("2026-07-20T12:10:00Z"), withStockMovements(current.items(), stockMovementIds, Map.of()));
            sales.remove(current);
            sales.add(confirmed);
            return Optional.of(confirmed);
        }

        @Override
        public Optional<Sale> cancelSale(TenantId tenantId, UUID id, UUID actorUserId, long version, String reason,
                                         List<SaleItemStockReversal> stockReversals, SalePaymentReversalUpdate paymentReversal) {
            Optional<Sale> existing = findById(tenantId, id).filter(sale -> sale.version() == version && sale.status().equals("CONFIRMED"));
            if (existing.isEmpty()) {
                return Optional.empty();
            }
            Sale current = existing.get();
            Map<UUID, UUID> reversals = new HashMap<>();
            stockReversals.forEach(reversal -> reversals.put(reversal.saleItemId(), reversal.stockReversalMovementId()));
            Sale cancelled = new Sale(current.id(), current.tenantId(), current.branchId(), current.customerId(), current.saleNumber(), current.numberCode(),
                "CANCELLED", current.currencyCode(), current.subtotalAmount(), current.totalAmount(), paymentReversal == null ? current.paymentStatus() : "REVERSED",
                current.paymentMethod(), current.paidAmount(), current.cashSessionId(), current.cashMovementId(),
                paymentReversal == null ? null : paymentReversal.cashSessionId(), paymentReversal == null ? null : paymentReversal.cashMovementId(),
                current.confirmedAt(), current.confirmedBy(), Instant.parse("2026-07-20T12:20:00Z"), actorUserId, reason, current.version() + 1,
                Instant.parse("2026-07-20T12:20:00Z"), withStockMovements(current.items(), Map.of(), reversals));
            sales.remove(current);
            sales.add(cancelled);
            return Optional.of(cancelled);
        }

        @Override
        public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId, String outcome,
                                     SalesRequestMetadata metadata, Map<String, Object> details) {
            auditActions.add(action + ":" + resourceType);
        }

        private List<SaleItem> toItems(UUID saleId, List<PreparedSaleItem> items, Map<UUID, UUID> stockMovementIds,
                                       Map<UUID, UUID> stockReversalMovementIds) {
            return items.stream()
                .map(item -> new SaleItem(UUID.randomUUID(), saleId, item.lineNumber(), item.productId(), item.warehouseId(), item.productSku(),
                    item.productName(), item.productType(), item.stockTrackingEnabled(), item.quantity(), item.unitPrice(), item.subtotalAmount(), null, null))
                .toList();
        }

        private List<SaleItem> withStockMovements(List<SaleItem> items, Map<UUID, UUID> stockMovementIds, Map<UUID, UUID> stockReversalMovementIds) {
            return items.stream()
                .map(item -> new SaleItem(item.id(), item.saleId(), item.lineNumber(), item.productId(), item.warehouseId(), item.productSku(),
                    item.productName(), item.productType(), item.stockTrackingEnabled(), item.quantity(), item.unitPrice(), item.subtotalAmount(),
                    stockMovementIds.getOrDefault(item.id(), item.stockMovementId()),
                    stockReversalMovementIds.getOrDefault(item.id(), item.stockReversalMovementId())))
                .toList();
        }
    }

    private record WarehouseKey(UUID branchId, UUID warehouseId) {
    }
}