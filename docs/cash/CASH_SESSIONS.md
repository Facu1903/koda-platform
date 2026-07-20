# Caja inicial

## Estado

Implementado en Sprint 2 Hito 4.

## Objetivo

El modulo de caja inicial permite abrir, operar y cerrar sesiones de caja por tenant, caja fisica/sucursal y usuario autenticado.

No es contabilidad, no es fiscalizacion y no reemplaza conciliacion bancaria. Es el ledger operativo minimo para que ventas y compras puedan impactar caja de forma explicita, trazable y auditable.

## Alcance funcional

- Caja por tenant y sucursal.
- Sesion de caja por caja y usuario.
- Una sola sesion abierta por caja y usuario.
- Apertura con saldo inicial.
- Cierre con saldo esperado, saldo contado y diferencia.
- Movimientos manuales permitidos: `CASH_IN` y `CASH_OUT`.
- Movimientos reservados para integraciones futuras: `SALE_PAYMENT`, `PURCHASE_PAYMENT`, `OPENING`, `CLOSING_ADJUSTMENT`.
- Medios de pago iniciales: `CASH`, `CARD`, `BANK_TRANSFER`, `OTHER`.

## API backend

Base path: `/api/v1/cash`.

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `GET` | `/registers` | Lista cajas activas del tenant. |
| `GET` | `/sessions` | Lista sesiones de caja, limitadas por permisos/rol. |
| `GET` | `/sessions/current` | Devuelve la sesion abierta del usuario actual, si existe. |
| `POST` | `/sessions/open` | Abre una sesion de caja. |
| `POST` | `/sessions/{id}/close` | Cierra una sesion de caja con version optimista. |
| `GET` | `/movements?sessionId={id}` | Lista movimientos de una sesion. |
| `POST` | `/movements` | Crea movimiento manual `CASH_IN` o `CASH_OUT`. |

Ningun endpoint acepta `tenant_id`. El tenant se resuelve desde JWT/Tenant Context.

## Permisos

| Permiso | Uso |
| --- | --- |
| `cash_registers:read` | Consultar cajas. |
| `cash_sessions:read` | Consultar sesiones. |
| `cash_sessions:open` | Abrir sesion. |
| `cash_sessions:close` | Cerrar sesion. |
| `cash_movements:read` | Consultar movimientos. |
| `cash_movements:create` | Crear movimientos manuales. |

Matriz aplicada para KODA:

- `TENANT_OWNER`, `TENANT_ADMIN`, `MANAGER`: acceso completo al modulo caja.
- `SALES_USER`: abre, cierra, lee y opera sus propias sesiones.
- `READ_ONLY`: solo lectura.
- `STOCK_USER`: sin acceso a caja en Sprint 2.

## Reglas de dinero

- Los importes se almacenan con escala 4.
- `amount` representa el importe informado del movimiento.
- `cash_effect` representa impacto real sobre efectivo fisico.
- Si el medio de pago no es `CASH`, `cash_effect` queda en `0.0000`.
- `CASH_IN` con `CASH` suma efectivo.
- `CASH_OUT` con `CASH` resta efectivo.
- El cierre calcula `expected_closing_amount` como suma de `cash_effect` de la sesion.
- `closing_difference = counted_closing_amount - expected_closing_amount`.
- Si la diferencia no es cero, se registra `CLOSING_ADJUSTMENT`.

## Persistencia

Migraciones:

- `V202607201100__create_cash_tables.sql`
- `V202607201110__seed_cash_permissions.sql`

Tablas principales:

- `cash_registers`
- `cash_sessions`
- `cash_movements`

Seed inicial para KODA:

- Caja: `CAJA_PRINCIPAL`.
- Sucursal: principal del tenant piloto.
- Modulo SaaS: `CASH`.

## Auditoria

Se auditan:

- Apertura de caja: `cash.session.open`.
- Cierre de caja: `cash.session.close`.
- Movimiento manual: `cash.movement.create`.

La auditoria guarda tenant, usuario actor, accion, recurso, IP, User-Agent y metadata relevante.

## Errores API

El manejador global devuelve errores estructurados para:

- `CASH_ITEM_NOT_FOUND`
- `CASH_VERSION_CONFLICT`
- `CASH_OPERATION_REJECTED`
- `PERMISSION_DENIED`
- `TENANT_CONTEXT_REQUIRED`
- `INVALID_REQUEST`

## Tests

Validado con:

- `mvn -B test`: 62 pruebas unitarias, 0 fallos.
- `mvn -B verify`: 62 pruebas unitarias y 3 pruebas de integracion, 0 fallos.
- Flyway/Testcontainers PostgreSQL 17.10 hasta `v202607201110`.

## Fuera de alcance

- Conciliacion bancaria.
- Cuentas corrientes.
- Caja multi-moneda operativa completa.
- Transferencias entre cajas.
- Arqueos parciales.
- Integracion fiscal.
- Contabilidad general.
- UI de caja.