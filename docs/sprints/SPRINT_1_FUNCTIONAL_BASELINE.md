# Sprint 1 - Functional Baseline

## Estado

Aprobado por Product Owner el 2026-07-15.

## Objetivo

Definir las decisiones funcionales minimas para iniciar Sprint 1 sin que el codigo invente reglas de negocio.

Esta base no reemplaza futuras especificaciones detalladas. Funciona como contrato inicial para construir autenticacion, multi-tenancy, configuracion de empresa, catalogos, stock y auditoria.

## Tenant inicial

| Campo | Valor |
| --- | --- |
| Nombre comercial | KODA |
| Razon social | KODA |
| Pais | Argentina |
| Idioma | es-AR |
| Moneda | ARS |
| Zona horaria | America/Argentina/Buenos_Aires |
| Tema por defecto | dark |
| Color primario | #F6862B |

## Roles iniciales

- `PLATFORM_SUPER_ADMIN`
- `TENANT_OWNER`
- `TENANT_ADMIN`
- `MANAGER`
- `SALES_USER`
- `STOCK_USER`
- `READ_ONLY`

## Permisos iniciales

| Recurso | Acciones |
| --- | --- |
| `users` | `read`, `create`, `update`, `delete` |
| `roles` | `read`, `create`, `update`, `delete` |
| `tenants` | `read`, `create`, `update` |
| `branches` | `read`, `create`, `update`, `delete` |
| `company_settings` | `read`, `update` |
| `products` | `read`, `create`, `update`, `delete` |
| `brands` | `read`, `create`, `update`, `delete` |
| `categories` | `read`, `create`, `update`, `delete` |
| `units` | `read`, `create`, `update`, `delete` |
| `presentations` | `read`, `create`, `update`, `delete` |
| `stock_balances` | `read` |
| `stock_movements` | `read`, `create` |
| `audit` | `read` |


## Decision funcional - Catalogos ERP

Aprobado por Product Owner el 2026-07-17 para Hito 6:

- Marca opcional en productos.
- Categorias planas para Sprint 1.
- Un producto tiene una presentacion principal.
- SKU unico por tenant.
- Se permite desactivar productos con stock.
- La eliminacion es soft delete.
- `TENANT_OWNER` y `TENANT_ADMIN`: CRUD completo de catalogos.
- `MANAGER`: lectura y actualizacion de catalogos.
- `READ_ONLY`: solo lectura de catalogos.

## Reglas iniciales de stock

- Tipos de movimiento: `IN`, `OUT`, `ADJUSTMENT`.
- No se permite stock negativo por defecto.
- Todo movimiento de stock debe ser auditado.
- Un movimiento confirmado no se edita.
- Para corregir un movimiento confirmado se debe generar un movimiento inverso.

## Decision funcional - Stock

Aprobado por Product Owner el 2026-07-17 para Hito 7:

- `IN` suma stock.
- `OUT` resta stock.
- `ADJUSTMENT` fija el stock al conteo real informado.
- No se permite stock negativo.
- No se permite dejar `quantity_on_hand < reserved_quantity`.
- No se permiten movimientos sobre productos `SERVICE`, inactivos o sin seguimiento de stock.
- Todo movimiento confirmado es inmutable.
- Toda correccion futura debe hacerse con otro movimiento.
- El ledger debe registrar saldo anterior, saldo posterior y delta.
- `TENANT_OWNER`, `TENANT_ADMIN`, `MANAGER` y `STOCK_USER`: lectura de saldos, lectura de movimientos y creacion de movimientos.
- `SALES_USER`: lectura de saldos.
- `READ_ONLY`: lectura de saldos y movimientos.
- Se crea seed minimo de sucursal/deposito KODA para operar Sprint 1 sin abrir todavia CRUD de sucursales/depositos.

## Criterios de implementacion

- El tenant se resuelve desde el contexto autenticado.
- El frontend no puede decidir tenant ni permisos efectivos.
- Los permisos deben validarse en backend.
- Todo endpoint tenant-scoped debe probar aislamiento entre empresas.
- Todo CRUD debe usar DTOs de request/response.
- Toda operacion sensible debe dejar traza de auditoria.

## Riesgos

1. Los roles iniciales son suficientes para arrancar, pero no deben convertirse en permisos hardcodeados.
2. La regla de no stock negativo simplifica Sprint 1, pero mas adelante puede requerir excepciones por modulo, sucursal o permiso.
3. `PLATFORM_SUPER_ADMIN` debe quedar separado de usuarios de tenant; mezclar ambos mundos seria una fuga de seguridad esperando horario de oficina.

## Decision

Se aprueba esta base para iniciar Sprint 1.

