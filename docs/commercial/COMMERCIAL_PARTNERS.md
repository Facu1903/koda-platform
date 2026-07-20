# Clientes y Proveedores

## Estado

Implementado en Sprint 2 Hito 3.

## Objetivo

Clientes y proveedores se exponen como conceptos separados para la operacion de KODA ERP, pero internamente comparten una base tecnica comun de terceros comerciales.

Esta decision evita duplicar datos de una misma persona o empresa cuando en el futuro pueda ser cliente, proveedor o ambos. La API mantiene claridad funcional; la base mantiene compatibilidad futura. Menos copia y pega, menos deuda con corbata.

## Modelo tecnico

Tablas principales:

- `business_partners`: datos comunes del tercero comercial.
- `business_partner_roles`: roles activos del tercero (`CUSTOMER`, `SUPPLIER`).

Un mismo `business_partner` puede tener ambos roles. Las APIs separadas filtran por rol.

## APIs

Clientes:

- `GET /api/v1/customers?limit=100`
- `GET /api/v1/customers/{id}`
- `POST /api/v1/customers`
- `PUT /api/v1/customers/{id}`
- `DELETE /api/v1/customers/{id}?version={version}`

Proveedores:

- `GET /api/v1/suppliers?limit=100`
- `GET /api/v1/suppliers/{id}`
- `POST /api/v1/suppliers`
- `PUT /api/v1/suppliers/{id}`
- `DELETE /api/v1/suppliers/{id}?version={version}`

Todas las rutas son tenant-scoped. El backend obtiene el tenant desde el JWT/Tenant Context. No se acepta `tenant_id` desde frontend.

## Campos

Campos principales:

- `legalName`: obligatorio.
- `commercialName`: opcional.
- `documentType`: opcional, pero si se informa requiere `documentNumber`.
- `documentNumber`: opcional, pero si se informa requiere `documentType`.
- `taxCondition`: opcional.
- `email`: opcional.
- `phone`: opcional.
- `addressLine`: opcional.
- `city`: opcional.
- `provinceCode`: opcional.
- `countryCode`: opcional, formato ISO 3166-1 alpha-2.
- `notes`: opcional.
- `status`: `ACTIVE` o `INACTIVE`.

## Reglas implementadas

- Cada registro pertenece a un tenant.
- `legalName` es obligatorio.
- `status` admite `ACTIVE` e `INACTIVE`.
- Documento fiscal es opcional, pero tipo y numero deben informarse juntos.
- La combinacion `documentType + documentNumber` es unica por tenant mientras el tercero no este eliminado.
- Email y telefono no son unicos en Sprint 2.
- Los listados tienen limite maximo de 500 registros por request.
- La eliminacion de cliente/proveedor elimina logicamente el rol, no borra fisicamente el tercero.
- Si se crea un proveedor con el mismo documento de un cliente existente, se adjunta el rol `SUPPLIER` al mismo tercero.
- Si se crea un cliente con el mismo documento de un proveedor existente, se adjunta el rol `CUSTOMER` al mismo tercero.
- Si el rol ya existe para ese documento, la operacion se rechaza con conflicto.

## Consumidor final

Flyway crea un cliente sistema `Consumidor Final` para el tenant piloto KODA.

Reglas:

- Tiene rol `CUSTOMER`.
- Es `system = true`.
- No se puede eliminar.
- No se puede desactivar.

## Permisos

Permisos atomicos agregados:

- `customers:read`
- `customers:create`
- `customers:update`
- `customers:delete`
- `suppliers:read`
- `suppliers:create`
- `suppliers:update`
- `suppliers:delete`

Matriz aplicada para KODA:

| Rol | Clientes | Proveedores |
| --- | --- | --- |
| `TENANT_OWNER` | CRUD | CRUD |
| `TENANT_ADMIN` | CRUD | CRUD |
| `MANAGER` | CRUD | CRUD |
| `SALES_USER` | Read/Create/Update | Read |
| `STOCK_USER` | Read | Read/Create/Update |
| `READ_ONLY` | Read | Read |

## Auditoria

Se auditan operaciones exitosas:

- `commercial.customer.create`
- `commercial.customer.update`
- `commercial.customer.delete`
- `commercial.supplier.create`
- `commercial.supplier.update`
- `commercial.supplier.delete`

## Versionado

La API usa version optimista. Las actualizaciones y eliminaciones requieren `version` para evitar pisar cambios concurrentes.

## Migraciones

- `V202607201000__create_commercial_partner_tables.sql`
- `V202607201010__seed_commercial_partners_permissions.sql`

El esquema queda en `v202607201010`.

## Validacion

Validado con:

- `mvn -B test`: 54 tests unitarios, 0 fallos.
- `mvn -B verify`: 54 tests unitarios y 3 tests de integracion, 0 fallos.
- Testcontainers con PostgreSQL 17.10 aplicando 12 migraciones desde cero.