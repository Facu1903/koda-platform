package com.koda.platform.platform.cash.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CashServiceTest {

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID cashRegisterId = UUID.randomUUID();
    private final FakeCashRepository repository = new FakeCashRepository();
    private final FakeCurrentTenantProvider currentTenantProvider = new FakeCurrentTenantProvider();
    private final CashRequestMetadata metadata = new CashRequestMetadata("127.0.0.1", "JUnit");
    private CashService service;

    @BeforeEach
    void setUp() {
        service = new CashService(repository, currentTenantProvider);
        repository.registers.add(new CashRegister(cashRegisterId, tenantId, branchId, "CAJA_PRINCIPAL", "Caja Principal", "ACTIVE", 0,
            Instant.parse("2026-07-20T10:00:00Z")));
        currentTenantProvider.context = Optional.of(new TenantContext(
            tenantId,
            userId,
            Set.of("TENANT_ADMIN"),
            Set.of(
                "cash_registers:read",
                "cash_sessions:read",
                "cash_sessions:open",
                "cash_sessions:close",
                "cash_movements:read",
                "cash_movements:create"
            ),
            false
        ));
    }

    @Test
    void openSessionUsesTenantCurrencyAndCreatesOpeningMovementAndAudit() {
        CashSession session = service.openSession(new OpenCashSessionCommand(cashRegisterId, new BigDecimal("100"), null), metadata);

        assertThat(session.currencyCode()).isEqualTo("ARS");
        assertThat(session.openingAmount()).isEqualByComparingTo("100.0000");
        assertThat(repository.movements).hasSize(1);
        assertThat(repository.movements.getFirst().movementType()).isEqualTo("OPENING");
        assertThat(repository.movements.getFirst().cashEffect()).isEqualByComparingTo("100.0000");
        assertThat(repository.auditActions).contains("cash.session.open:cash_session");
    }

    @Test
    void openSessionRejectsDuplicateForSameRegisterAndUser() {
        service.openSession(new OpenCashSessionCommand(cashRegisterId, new BigDecimal("0"), "ARS"), metadata);

        assertThatThrownBy(() -> service.openSession(new OpenCashSessionCommand(cashRegisterId, new BigDecimal("0"), "ARS"), metadata))
            .isInstanceOf(CashOperationRejectedException.class)
            .satisfies(exception -> assertThat(((CashOperationRejectedException) exception).reasonCode()).isEqualTo("CASH_SESSION_ALREADY_OPEN"));
    }

    @Test
    void salesUserListsOnlyOwnSessions() {
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of("SALES_USER"), Set.of("cash_sessions:read"), false));
        CashSession ownSession = repository.addSession(userId, "OPEN", new BigDecimal("0"));
        CashSession otherSession = repository.addSession(otherUserId, "OPEN", new BigDecimal("0"));

        List<CashSession> sessions = service.listSessions(100);

        assertThat(sessions).extracting(CashSession::id).contains(ownSession.id()).doesNotContain(otherSession.id());
    }

    @Test
    void createNonCashManualMovementDoesNotAffectCashExpected() {
        CashSession session = service.openSession(new OpenCashSessionCommand(cashRegisterId, BigDecimal.ZERO, "ARS"), metadata);

        CashMovement movement = service.createMovement(new CreateCashMovementCommand(session.id(), "cash_in", "card", new BigDecimal("100"), "ARS",
            null, null, "Card income reference"), metadata);

        assertThat(movement.paymentMethod()).isEqualTo("CARD");
        assertThat(movement.amount()).isEqualByComparingTo("100.0000");
        assertThat(movement.cashEffect()).isEqualByComparingTo("0.0000");
        assertThat(repository.sumCashEffect(tenantId, session.id(), "ARS")).isEqualByComparingTo("0.0000");
    }

    @Test
    void createMovementRejectsClosedSession() {
        CashSession closedSession = repository.addSession(userId, "CLOSED", new BigDecimal("0"));

        assertThatThrownBy(() -> service.createMovement(new CreateCashMovementCommand(closedSession.id(), "CASH_OUT", "CASH",
            new BigDecimal("10"), "ARS", null, null, null), metadata))
            .isInstanceOf(CashOperationRejectedException.class)
            .satisfies(exception -> assertThat(((CashOperationRejectedException) exception).reasonCode()).isEqualTo("CASH_SESSION_CLOSED"));
    }

    @Test
    void closeSessionStoresExpectedAmountAndCreatesClosingAdjustment() {
        CashSession session = service.openSession(new OpenCashSessionCommand(cashRegisterId, new BigDecimal("100"), "ARS"), metadata);
        service.createMovement(new CreateCashMovementCommand(session.id(), "CASH_OUT", "CASH", new BigDecimal("20"), "ARS", null, null,
            "Petty cash expense"), metadata);

        CashSession closed = service.closeSession(session.id(), new CloseCashSessionCommand(session.version(), new BigDecimal("90")), metadata);

        assertThat(closed.status()).isEqualTo("CLOSED");
        assertThat(closed.expectedClosingAmount()).isEqualByComparingTo("80.0000");
        assertThat(closed.countedClosingAmount()).isEqualByComparingTo("90.0000");
        assertThat(closed.closingDifference()).isEqualByComparingTo("10.0000");
        assertThat(repository.movements).extracting(CashMovement::movementType).contains("CLOSING_ADJUSTMENT");
        CashMovement adjustment = repository.movements.stream()
            .filter(movement -> movement.movementType().equals("CLOSING_ADJUSTMENT"))
            .findFirst()
            .orElseThrow();
        assertThat(adjustment.amount()).isEqualByComparingTo("10.0000");
        assertThat(adjustment.cashEffect()).isEqualByComparingTo("10.0000");
        assertThat(repository.auditActions).contains("cash.session.close:cash_session");
    }

    @Test
    void createMovementRejectsReservedSalePaymentManualType() {
        CashSession session = service.openSession(new OpenCashSessionCommand(cashRegisterId, BigDecimal.ZERO, "ARS"), metadata);

        assertThatThrownBy(() -> service.createMovement(new CreateCashMovementCommand(session.id(), "SALE_PAYMENT", "CASH",
            new BigDecimal("10"), "ARS", null, null, null), metadata))
            .isInstanceOf(CashOperationRejectedException.class)
            .satisfies(exception -> assertThat(((CashOperationRejectedException) exception).reasonCode()).isEqualTo("UNSUPPORTED_MANUAL_MOVEMENT_TYPE"));
    }

    @Test
    void salesUserCannotOperateAnotherUsersSession() {
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of("SALES_USER"), Set.of("cash_movements:create"), false));
        CashSession otherSession = repository.addSession(otherUserId, "OPEN", BigDecimal.ZERO);

        assertThatThrownBy(() -> service.createMovement(new CreateCashMovementCommand(otherSession.id(), "CASH_IN", "CASH",
            new BigDecimal("10"), "ARS", null, null, null), metadata))
            .isInstanceOf(PermissionDeniedException.class);
    }

    private final class FakeCurrentTenantProvider implements CurrentTenantProvider {
        private Optional<TenantContext> context = Optional.empty();

        @Override
        public Optional<TenantContext> currentContext() {
            return context;
        }
    }

    private final class FakeCashRepository implements CashRepository {
        private final List<CashRegister> registers = new ArrayList<>();
        private final List<CashSession> sessions = new ArrayList<>();
        private final List<CashMovement> movements = new ArrayList<>();
        private final List<String> auditActions = new ArrayList<>();

        @Override
        public List<CashRegister> listRegisters(TenantId tenantId) {
            return registers.stream().filter(register -> register.tenantId().equals(tenantId)).toList();
        }

        @Override
        public Optional<CashRegister> findActiveRegister(TenantId tenantId, UUID id) {
            return registers.stream()
                .filter(register -> register.tenantId().equals(tenantId) && register.id().equals(id) && register.status().equals("ACTIVE"))
                .findFirst();
        }

        @Override
        public Optional<String> findTenantCurrency(TenantId tenantId) {
            return Optional.of("ARS");
        }

        @Override
        public List<CashSession> listSessions(TenantId tenantId, UUID openedByUserId, int limit) {
            return sessions.stream()
                .filter(session -> session.tenantId().equals(tenantId))
                .filter(session -> openedByUserId == null || session.openedByUserId().equals(openedByUserId))
                .limit(limit)
                .toList();
        }

        @Override
        public Optional<CashSession> findSessionById(TenantId tenantId, UUID id) {
            return sessions.stream().filter(session -> session.tenantId().equals(tenantId) && session.id().equals(id)).findFirst();
        }

        @Override
        public Optional<CashSession> findCurrentOpenSession(TenantId tenantId, UUID openedByUserId) {
            return sessions.stream()
                .filter(session -> session.tenantId().equals(tenantId))
                .filter(session -> session.openedByUserId().equals(openedByUserId))
                .filter(session -> session.status().equals("OPEN"))
                .findFirst();
        }

        @Override
        public boolean existsOpenSession(TenantId tenantId, UUID cashRegisterId, UUID openedByUserId) {
            return sessions.stream()
                .anyMatch(session -> session.tenantId().equals(tenantId)
                    && session.cashRegisterId().equals(cashRegisterId)
                    && session.openedByUserId().equals(openedByUserId)
                    && session.status().equals("OPEN"));
        }

        @Override
        public CashSession createSession(TenantId tenantId, UUID actorUserId, CashRegister cashRegister, OpenCashSessionCommand command) {
            CashSession session = new CashSession(
                UUID.randomUUID(),
                tenantId,
                cashRegister.id(),
                cashRegister.branchId(),
                actorUserId,
                "OPEN",
                money(command.openingAmount()),
                command.currencyCode(),
                null,
                null,
                null,
                Instant.parse("2026-07-20T10:00:00Z"),
                null,
                0,
                Instant.parse("2026-07-20T10:00:00Z")
            );
            sessions.add(session);
            return session;
        }

        @Override
        public Optional<CashSession> closeSession(TenantId tenantId, UUID id, UUID actorUserId, CloseCashSessionCommand command,
                                                  BigDecimal expectedClosingAmount, BigDecimal closingDifference) {
            Optional<CashSession> existing = findSessionById(tenantId, id)
                .filter(session -> session.status().equals("OPEN") && session.version() == command.version());
            if (existing.isEmpty()) {
                return Optional.empty();
            }
            CashSession current = existing.get();
            CashSession closed = new CashSession(
                current.id(),
                current.tenantId(),
                current.cashRegisterId(),
                current.branchId(),
                current.openedByUserId(),
                "CLOSED",
                current.openingAmount(),
                current.currencyCode(),
                expectedClosingAmount,
                command.countedClosingAmount(),
                closingDifference,
                current.openedAt(),
                Instant.parse("2026-07-20T10:30:00Z"),
                current.version() + 1,
                Instant.parse("2026-07-20T10:30:00Z")
            );
            sessions.remove(current);
            sessions.add(closed);
            return Optional.of(closed);
        }

        @Override
        public List<CashMovement> listMovements(TenantId tenantId, UUID cashSessionId, int limit) {
            return movements.stream()
                .filter(movement -> movement.tenantId().equals(tenantId) && movement.cashSessionId().equals(cashSessionId))
                .limit(limit)
                .toList();
        }

        @Override
        public CashMovement createMovement(TenantId tenantId, UUID actorUserId, CashSession cashSession, CreateCashMovementCommand command,
                                           BigDecimal cashEffect) {
            CashMovement movement = new CashMovement(
                UUID.randomUUID(),
                tenantId,
                cashSession.id(),
                cashSession.cashRegisterId(),
                cashSession.branchId(),
                command.movementType(),
                command.paymentMethod(),
                money(command.amount()),
                money(cashEffect),
                command.currencyCode(),
                command.referenceType(),
                command.referenceId(),
                command.description(),
                actorUserId,
                Instant.parse("2026-07-20T10:05:00Z")
            );
            movements.add(movement);
            return movement;
        }

        @Override
        public BigDecimal sumCashEffect(TenantId tenantId, UUID cashSessionId, String currencyCode) {
            return movements.stream()
                .filter(movement -> movement.tenantId().equals(tenantId))
                .filter(movement -> movement.cashSessionId().equals(cashSessionId))
                .filter(movement -> movement.currencyCode().equals(currencyCode))
                .map(CashMovement::cashEffect)
                .reduce(BigDecimal.ZERO.setScale(4), BigDecimal::add)
                .setScale(4, RoundingMode.UNNECESSARY);
        }

        @Override
        public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                                     String outcome, CashRequestMetadata metadata, Map<String, Object> details) {
            auditActions.add(action + ":" + resourceType);
        }

        private CashSession addSession(UUID openedByUserId, String status, BigDecimal openingAmount) {
            CashSession session = new CashSession(
                UUID.randomUUID(),
                tenantId,
                cashRegisterId,
                branchId,
                openedByUserId,
                status,
                money(openingAmount),
                "ARS",
                status.equals("CLOSED") ? money(openingAmount) : null,
                status.equals("CLOSED") ? money(openingAmount) : null,
                status.equals("CLOSED") ? BigDecimal.ZERO.setScale(4) : null,
                Instant.parse("2026-07-20T09:00:00Z"),
                status.equals("CLOSED") ? Instant.parse("2026-07-20T09:30:00Z") : null,
                0,
                Instant.parse("2026-07-20T09:00:00Z")
            );
            sessions.add(session);
            return session;
        }

        private BigDecimal money(BigDecimal value) {
            return value.setScale(4, RoundingMode.UNNECESSARY);
        }
    }
}