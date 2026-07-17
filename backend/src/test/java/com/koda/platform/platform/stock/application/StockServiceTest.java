package com.koda.platform.platform.stock.application;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockServiceTest {

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final TenantId otherTenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000099"));
    private final UUID userId = UUID.randomUUID();
    private final UUID warehouseId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final FakeStockRepository repository = new FakeStockRepository();
    private final FakeCurrentTenantProvider currentTenantProvider = new FakeCurrentTenantProvider();
    private final StockRequestMetadata metadata = new StockRequestMetadata("127.0.0.1", "JUnit");
    private StockService service;

    @BeforeEach
    void setUp() {
        service = new StockService(repository, currentTenantProvider);
        currentTenantProvider.context = Optional.of(new TenantContext(
            tenantId,
            userId,
            Set.of("TENANT_ADMIN"),
            Set.of("stock_balances:read", "stock_movements:read", "stock_movements:create"),
            false
        ));
        repository.activeWarehouses.add(warehouseId);
        repository.products.put(productId, new StockProduct(productId, "GOOD", "ACTIVE", true, false));
    }

    @Test
    void createInMovementCreatesBalanceAndRecordsAudit() {
        StockMovement movement = service.createMovement(command("in", "10"), metadata);

        assertThat(movement.movementType()).isEqualTo("IN");
        assertThat(movement.quantityBefore()).isEqualByComparingTo("0.000000");
        assertThat(movement.quantityAfter()).isEqualByComparingTo("10.000000");
        assertThat(movement.quantityDelta()).isEqualByComparingTo("10.000000");
        assertThat(repository.balances.get(new BalanceKey(warehouseId, productId)).quantityOnHand()).isEqualByComparingTo("10.000000");
        assertThat(repository.auditActions).contains("stock.movement.create:stock_movement");
    }

    @Test
    void createOutMovementRejectsNegativeStock() {
        seedBalance("3", "0");

        assertThatThrownBy(() -> service.createMovement(command("OUT", "5"), metadata))
            .isInstanceOf(StockMovementRejectedException.class)
            .satisfies(exception -> assertThat(((StockMovementRejectedException) exception).reasonCode()).isEqualTo("INSUFFICIENT_STOCK"));
    }

    @Test
    void createAdjustmentCanSetStockToZero() {
        seedBalance("5", "0");

        StockMovement movement = service.createMovement(command("ADJUSTMENT", "0"), metadata);

        assertThat(movement.quantity()).isEqualByComparingTo("0.000000");
        assertThat(movement.quantityBefore()).isEqualByComparingTo("5.000000");
        assertThat(movement.quantityAfter()).isEqualByComparingTo("0.000000");
        assertThat(movement.quantityDelta()).isEqualByComparingTo("-5.000000");
    }

    @Test
    void createMovementRejectsReservedStockConflict() {
        seedBalance("5", "3");

        assertThatThrownBy(() -> service.createMovement(command("OUT", "4"), metadata))
            .isInstanceOf(StockMovementRejectedException.class)
            .satisfies(exception -> assertThat(((StockMovementRejectedException) exception).reasonCode()).isEqualTo("RESERVED_STOCK_CONFLICT"));
    }

    @Test
    void createMovementRejectsServiceProduct() {
        repository.products.put(productId, new StockProduct(productId, "SERVICE", "ACTIVE", true, false));

        assertThatThrownBy(() -> service.createMovement(command("IN", "1"), metadata))
            .isInstanceOf(StockMovementRejectedException.class)
            .satisfies(exception -> assertThat(((StockMovementRejectedException) exception).reasonCode()).isEqualTo("PRODUCT_NOT_STOCKABLE"));
    }

    @Test
    void listBalancesUsesCurrentTenantOnlyAndCapsLimit() {
        UUID foreignWarehouseId = UUID.randomUUID();
        UUID foreignProductId = UUID.randomUUID();
        seedBalance("2", "0");
        repository.balances.put(new BalanceKey(foreignWarehouseId, foreignProductId), new StockBalance(
            UUID.randomUUID(),
            otherTenantId,
            foreignWarehouseId,
            foreignProductId,
            new BigDecimal("99").setScale(6),
            BigDecimal.ZERO.setScale(6),
            0,
            Instant.parse("2026-07-17T18:00:00Z")
        ));

        List<StockBalance> balances = service.listBalances(null, null, 900);

        assertThat(repository.lastBalanceLimit).isEqualTo(500);
        assertThat(balances).extracting(StockBalance::tenantId).containsOnly(tenantId);
        assertThat(balances).extracting(StockBalance::warehouseId).doesNotContain(foreignWarehouseId);
    }

    @Test
    void createMovementRejectsMissingPermission() {
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of(), Set.of("stock_balances:read"), false));

        assertThatThrownBy(() -> service.createMovement(command("IN", "1"), metadata))
            .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void listBalancesRequiresReadPermission() {
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of(), Set.of("stock_movements:read"), false));

        assertThatThrownBy(() -> service.listBalances(null, null, 100))
            .isInstanceOf(PermissionDeniedException.class);
    }

    private CreateStockMovementCommand command(String movementType, String quantity) {
        return new CreateStockMovementCommand(warehouseId, productId, movementType, new BigDecimal(quantity), null, null, null, null, null);
    }

    private void seedBalance(String quantityOnHand, String reservedQuantity) {
        StockBalance balance = new StockBalance(
            UUID.randomUUID(),
            tenantId,
            warehouseId,
            productId,
            new BigDecimal(quantityOnHand).setScale(6),
            new BigDecimal(reservedQuantity).setScale(6),
            0,
            Instant.parse("2026-07-17T18:00:00Z")
        );
        repository.balances.put(new BalanceKey(warehouseId, productId), balance);
    }

    private final class FakeCurrentTenantProvider implements CurrentTenantProvider {
        private Optional<TenantContext> context = Optional.empty();

        @Override
        public Optional<TenantContext> currentContext() {
            return context;
        }
    }

    private final class FakeStockRepository implements StockRepository {
        private final Set<UUID> activeWarehouses = new HashSet<>();
        private final Map<UUID, StockProduct> products = new HashMap<>();
        private final Map<BalanceKey, StockBalance> balances = new HashMap<>();
        private final List<StockMovement> movements = new ArrayList<>();
        private final List<String> auditActions = new ArrayList<>();
        private int lastBalanceLimit;

        @Override
        public List<StockBalance> listBalances(TenantId tenantId, UUID warehouseId, UUID productId, int limit) {
            lastBalanceLimit = limit;
            return balances.values().stream()
                .filter(balance -> balance.tenantId().equals(tenantId))
                .filter(balance -> warehouseId == null || balance.warehouseId().equals(warehouseId))
                .filter(balance -> productId == null || balance.productId().equals(productId))
                .limit(limit)
                .toList();
        }

        @Override
        public List<StockMovement> listMovements(TenantId tenantId, UUID warehouseId, UUID productId, int limit) {
            return movements.stream()
                .filter(movement -> movement.tenantId().equals(tenantId))
                .filter(movement -> warehouseId == null || movement.warehouseId().equals(warehouseId))
                .filter(movement -> productId == null || movement.productId().equals(productId))
                .limit(limit)
                .toList();
        }

        @Override
        public Optional<StockMovement> findMovementById(TenantId tenantId, UUID id) {
            return movements.stream().filter(movement -> movement.tenantId().equals(tenantId) && movement.id().equals(id)).findFirst();
        }

        @Override
        public Optional<StockProduct> findProductForStock(TenantId tenantId, UUID id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public boolean existsActiveWarehouse(TenantId tenantId, UUID id) {
            return activeWarehouses.contains(id);
        }

        @Override
        public StockBalance lockOrCreateBalance(TenantId tenantId, UUID warehouseId, UUID productId) {
            BalanceKey key = new BalanceKey(warehouseId, productId);
            return balances.computeIfAbsent(key, ignored -> new StockBalance(
                UUID.randomUUID(),
                tenantId,
                warehouseId,
                productId,
                BigDecimal.ZERO.setScale(6),
                BigDecimal.ZERO.setScale(6),
                0,
                Instant.parse("2026-07-17T18:00:00Z")
            ));
        }

        @Override
        public StockBalance updateBalanceQuantity(TenantId tenantId, UUID balanceId, BigDecimal quantityOnHand) {
            Map.Entry<BalanceKey, StockBalance> entry = balances.entrySet().stream()
                .filter(item -> item.getValue().tenantId().equals(tenantId) && item.getValue().id().equals(balanceId))
                .findFirst()
                .orElseThrow();
            StockBalance current = entry.getValue();
            StockBalance updated = new StockBalance(
                current.id(),
                current.tenantId(),
                current.warehouseId(),
                current.productId(),
                quantityOnHand,
                current.reservedQuantity(),
                current.version() + 1,
                Instant.parse("2026-07-17T18:01:00Z")
            );
            balances.put(entry.getKey(), updated);
            return updated;
        }

        @Override
        public StockMovement createMovement(TenantId tenantId, UUID actorUserId, CreateStockMovementCommand command,
                                            BigDecimal quantityBefore, BigDecimal quantityAfter, BigDecimal quantityDelta) {
            StockMovement movement = new StockMovement(
                UUID.randomUUID(),
                tenantId,
                command.warehouseId(),
                command.productId(),
                command.movementType(),
                command.quantity(),
                quantityBefore,
                quantityAfter,
                quantityDelta,
                command.unitCost(),
                command.currencyCode(),
                command.referenceType(),
                command.referenceId(),
                null,
                command.reason(),
                Instant.parse("2026-07-17T18:02:00Z"),
                actorUserId
            );
            movements.add(movement);
            return movement;
        }

        @Override
        public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                                     String outcome, StockRequestMetadata metadata, Map<String, Object> details) {
            auditActions.add(action + ":" + resourceType);
        }
    }

    private record BalanceKey(UUID warehouseId, UUID productId) {
    }
}