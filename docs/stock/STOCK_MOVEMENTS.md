# Stock Movements

## Objetivo

El Hito 7 implementa la base operativa de stock de KODA ERP: saldos por deposito/producto y movimientos confirmados como ledger inmutable.

Todos los endpoints son tenant-scoped. El tenant efectivo se resuelve desde `TenantContext`; el cliente no envia ni decide `tenant_id`.

## Base funcional aprobada

Aprobado por Product Owner el 2026-07-17:

- Tipos de movimiento: `IN`, `OUT`, `ADJUSTMENT`.
- `IN` suma stock.
- `OUT` resta stock.
- `ADJUSTMENT` fija el stock al conteo real informado.
- No se permite stock negativo.
- No se permite `OUT` ni `ADJUSTMENT` que deje `quantity_on_hand < reserved_quantity`.
- No se permiten movimientos sobre productos `SERVICE`, inactivos o sin seguimiento de stock.
- Todo movimiento confirmado es inmutable.
- Toda correccion futura debe hacerse con otro movimiento.
- El ledger guarda `quantity_before`, `quantity_after` y `quantity_delta`.

## Endpoints

Base path: `/api/v1/stock`.

### Balances

- `GET /balances`

Parametros opcionales:

- `warehouseId`
- `productId`
- `limit`, valor por defecto `200`, maximo `500`

### Movements

- `GET /movements`
- `GET /movements/{id}`
- `POST /movements`

Parametros opcionales de listado:

- `warehouseId`
- `productId`
- `limit`, valor por defecto `100`, maximo `500`

## Ejemplo de movimiento

```json
{
  "warehouseId": "40000000-0000-4000-8000-000000000101",
  "productId": "00000000-0000-4000-8000-000000000999",
  "movementType": "IN",
  "quantity": 10,
  "unitCost": null,
  "currencyCode": null,
  "referenceType": null,
  "referenceId": null,
  "reason": "Carga inicial"
}
```

## Permisos

| Rol | Permisos de stock |
| --- | --- |
| `TENANT_OWNER` | `stock_balances:read`, `stock_movements:read`, `stock_movements:create` |
| `TENANT_ADMIN` | `stock_balances:read`, `stock_movements:read`, `stock_movements:create` |
| `MANAGER` | `stock_balances:read`, `stock_movements:read`, `stock_movements:create` |
| `STOCK_USER` | `stock_balances:read`, `stock_movements:read`, `stock_movements:create` |
| `SALES_USER` | `stock_balances:read` |
| `READ_ONLY` | `stock_balances:read`, `stock_movements:read` |

La matriz se aplica al tenant piloto KODA mediante Flyway en `V202607171540__enable_stock_operations.sql`.

## Reglas tecnicas

- `stock_movements` es el ledger confirmado e inmutable.
- `stock_balances` mantiene el saldo materializado para consulta rapida.
- La aplicacion bloquea el saldo con `SELECT ... FOR UPDATE` antes de calcular y confirmar el movimiento.
- `IN` y `OUT` requieren cantidad mayor que cero.
- `ADJUSTMENT` permite cantidad cero.
- `unitCost` y `currencyCode` deben enviarse juntos o ambos omitirse.
- `referenceType` y `referenceId` deben enviarse juntos o ambos omitirse.
- El deposito debe existir, estar activo y pertenecer al tenant autenticado.
- El producto debe existir, estar activo, ser `GOOD` y tener `stockTrackingEnabled = true`.
- Las operaciones sensibles registran auditoria en `audit_events`.

## Seed operativo KODA

La migracion crea una base minima para operar stock en el tenant piloto:

- Sucursal `CENTRAL`.
- Deposito `PRINCIPAL` con id `40000000-0000-4000-8000-000000000101`.

Esto no reemplaza el futuro CRUD de sucursales/depositos.

## Errores

- `401`: falta autenticacion o token valido.
- `403 PERMISSION_DENIED`: el JWT no contiene el permiso requerido.
- `404 STOCK_ITEM_NOT_FOUND`: el movimiento solicitado no existe para el tenant autenticado.
- `400 STOCK_REFERENCE_NOT_FOUND`: deposito o producto inexistente, inactivo o fuera del tenant.
- `409 STOCK_MOVEMENT_REJECTED`: regla de stock rechazada, por ejemplo stock insuficiente o producto no stockeable.
- `400 VALIDATION_ERROR`: request invalido por Bean Validation.
- `400 INVALID_REQUEST`: regla de aplicacion invalida.

## No incluido en Hito 7

- Transferencias entre depositos.
- Reservas de stock.
- Lotes.
- Vencimientos.
- Costos promedio.
- Valorizacion contable.
- UI de stock.
- CRUD de sucursales/depositos.