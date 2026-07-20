package com.koda.platform.platform.cash.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CashRepository {

    List<CashRegister> listRegisters(TenantId tenantId);

    Optional<CashRegister> findActiveRegister(TenantId tenantId, UUID id);

    Optional<String> findTenantCurrency(TenantId tenantId);

    List<CashSession> listSessions(TenantId tenantId, UUID openedByUserId, int limit);

    Optional<CashSession> findSessionById(TenantId tenantId, UUID id);

    Optional<CashSession> findCurrentOpenSession(TenantId tenantId, UUID openedByUserId);

    boolean existsOpenSession(TenantId tenantId, UUID cashRegisterId, UUID openedByUserId);

    CashSession createSession(TenantId tenantId, UUID actorUserId, CashRegister cashRegister, OpenCashSessionCommand command);

    Optional<CashSession> closeSession(TenantId tenantId, UUID id, UUID actorUserId, CloseCashSessionCommand command,
                                       BigDecimal expectedClosingAmount, BigDecimal closingDifference);

    List<CashMovement> listMovements(TenantId tenantId, UUID cashSessionId, int limit);

    CashMovement createMovement(TenantId tenantId, UUID actorUserId, CashSession cashSession, CreateCashMovementCommand command,
                                BigDecimal cashEffect);

    BigDecimal sumCashEffect(TenantId tenantId, UUID cashSessionId, String currencyCode);

    void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                          String outcome, CashRequestMetadata metadata, Map<String, Object> details);
}