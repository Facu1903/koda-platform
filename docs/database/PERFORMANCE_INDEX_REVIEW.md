# Performance e Indices Criticos

## Estado

Implementado en Sprint 4 Hito 5.

## Objetivo

Revisar consultas criticas de KODA PLATFORM y agregar solo indices justificados por queries existentes.

Regla aplicada: cada indice debe responder a una consulta concreta. Un indice sin consulta real es deuda tecnica con corbata.

## Alcance revisado

Se revisaron las consultas de:

- capabilities y guards de licencia,
- feature flags efectivos,
- auditoria tenant-scoped,
- reportes comerciales,
- listas operativas de ventas, compras, stock y caja.

No se incorporaron motores externos ni cambios funcionales.

## Migracion

La migracion agregada es:

- `V202607211900__add_operational_performance_indexes.sql`

Flyway deja el schema en:

- `v202607211900`

## Indices agregados o ajustados

| Indice | Tabla | Consulta que justifica | Motivo |
| --- | --- | --- | --- |
| `idx_tenant_product_subscriptions_active_validity` | `tenant_product_subscriptions` | `GET /api/v1/capabilities` y guards `isProductEnabled`/`isModuleEnabled` | Reduce busquedas de suscripciones activas por tenant/producto y vigencia. |
| `idx_tenant_feature_flags_effective` | `tenant_feature_flags` | `findEffectiveFeatureFlags` | Soporta lectura de flags efectivos por tenant/producto/modulo y vigencia, incluyendo flags habilitados y deshabilitados. |
| `idx_audit_events_tenant_actor_occurred_at` | `audit_events` | `/api/v1/audit/events?actorUserId=...` | Evita depender de un indice global por actor y mantiene el filtro tenant-scoped. |
| `idx_audit_events_tenant_resource_occurred_at` | `audit_events` | `/api/v1/audit/events?resourceType=...&resourceId=...` | Optimiza investigacion de un recurso dentro del tenant y conserva orden temporal. |
| `idx_audit_events_tenant_action_occurred_at` | `audit_events` | `/api/v1/audit/events?action=...` | Optimiza busquedas por accion funcional dentro del tenant. |
| `idx_sales_orders_tenant_confirmed_at` | `sales_orders` | reportes de ventas por rango | Se ajusto para ordenar por `confirmed_at DESC, sale_number DESC` sin sort extra relevante. |
| `idx_purchase_orders_tenant_confirmed_at` | `purchase_orders` | reportes de compras por rango | Se ajusto para ordenar por `confirmed_at DESC, purchase_number DESC` sin sort extra relevante. |
| `idx_stock_movements_tenant_confirmed_at` | `stock_movements` | dashboard de ultimos movimientos | Se ajusto para ordenar por `confirmed_at DESC, id DESC`. |
| `idx_sales_orders_tenant_updated_at` | `sales_orders` | listado operativo de ventas | Cubre `WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT ?`. |
| `idx_purchase_orders_tenant_updated_at` | `purchase_orders` | listado operativo de compras | Cubre `WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT ?`. |
| `idx_stock_balances_tenant_updated_at` | `stock_balances` | listado operativo de saldos | Cubre listado por tenant ordenado por actualizacion. |
| `idx_cash_sessions_tenant_user_open` | `cash_sessions` | caja actual del usuario | Acelera busqueda de sesion abierta por tenant/usuario. |

## Decisiones conservadoras

No se agrego un indice para cada filtro posible.

Decisiones explicitas:

- No se agrego indice separado por `outcome` en auditoria porque tiene baja cardinalidad y puede filtrar sobre indices temporales existentes.
- No se agregaron mas indices sobre `tenant_product_entitlements` ni `tenant_module_entitlements` porque las restricciones unicas `(tenant_id, product_id)` y `(tenant_id, module_id)` ya cubren los joins principales.
- No se implemento particionamiento de auditoria en este hito; se tratara en el hito de auditoria operativa.
- No se eliminaron indices existentes salvo reemplazos controlados de indices de reportes por definiciones equivalentes mas completas.

## Riesgo controlado

Los indices nuevos mejoran lectura, pero aumentan costo de escritura.

Tablas con mayor sensibilidad:

- `audit_events`: alta escritura futura.
- `sales_orders`: escritura media/alta.
- `purchase_orders`: escritura media.
- `stock_movements`: alta escritura futura.

Por eso se priorizaron consultas reales y se evitaron indices redundantes por filtros de baja utilidad.

## Validacion

Validaciones ejecutadas:

```powershell
mvn -B "-Dtest=NoUnitTests" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dit.test=FlywayPostgresqlIT" verify
```

Resultado:

- 22 migraciones aplicadas correctamente en PostgreSQL 17 real.
- Schema final `v202607211900`.
- 12 pruebas de integracion de Flyway/PostgreSQL, 0 fallos.

La validacion completa del backend se ejecuta al cierre del hito con:

```powershell
mvn -B verify
```

Resultado completo:

- 130 pruebas unitarias, 0 fallos.
- 14 pruebas de integracion, 0 fallos.
