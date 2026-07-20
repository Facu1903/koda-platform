# Tenant License Guards

## Estado

Implementado en Sprint 3 Hito 4.

## Objetivo

Bloquear operaciones backend cuando el tenant autenticado no tiene habilitado el producto o modulo requerido, aunque el usuario tenga permisos RBAC.

RBAC responde: que puede hacer este usuario.

Licenciamiento SaaS responde: que tiene contratado o habilitado esta empresa.

Las dos condiciones deben cumplirse.

## Componentes

- `TenantLicenseAccessGuard`: servicio de aplicacion reutilizable para validar producto/modulo.
- `TenantLicenseAccessRepository`: puerto de aplicacion para resolver acceso efectivo.
- `JdbcTenantCapabilitiesRepository`: implementa consultas `EXISTS` para producto y modulo habilitado.
- `TenantLicenseAccessDeniedException`: error de aplicacion para bloqueos de licencia.

## Errores API

Los bloqueos devuelven HTTP 403 con `ProblemDetail`.

Codigo principal:

- `TENANT_LICENSE_ACCESS_DENIED`

Motivos:

- `PRODUCT_NOT_ENABLED`
- `MODULE_NOT_ENABLED`

Propiedades incluidas:

- `reasonCode`
- `productCode`
- `moduleCode`
- `timestamp`
- `path`

## Orden de validacion

Para operaciones tenant-scoped:

1. Resolver `TenantContext`.
2. Validar producto/modulo del tenant.
3. Validar permiso RBAC.
4. Ejecutar la operacion.

Esta decision evita que un permiso de usuario se confunda con una licencia comercial. Un `PLATFORM_SUPER_ADMIN` puede saltar RBAC en operaciones existentes, pero no salta la licencia del tenant cuando opera dentro de una empresa.

## Modulos protegidos

| Servicio | Producto | Modulo |
| --- | --- | --- |
| `CompanySettingsService` | `KODA_ERP` | `CONFIGURATION` |
| `CatalogService` | `KODA_ERP` | `CATALOGS` |
| `StockService` | `KODA_ERP` | `STOCK` |
| `AuditService` | `KODA_ERP` | `AUDIT` |
| `CommercialPartnerService` | `KODA_ERP` | `COMMERCIAL_PARTNERS` |
| `CashService` | `KODA_ERP` | `CASH` |
| `SalesService` | `KODA_ERP` | `SALES` |
| `PurchasesService` | `KODA_ERP` | `PURCHASES` |
| `ReportsService` | `KODA_ERP` | `COMMERCIAL_REPORTS` |

## Dependencias cruzadas

Ventas y compras pueden invocar stock y caja de forma interna.

Reglas actuales:

- `SalesService` requiere `SALES` para operar ventas.
- Si una venta confirma o revierte stock, requiere tambien `STOCK`.
- Si una venta registra o revierte pago por caja, requiere tambien `CASH`.
- `PurchasesService` requiere `PURCHASES` para operar compras.
- Si una compra confirma o revierte stock, requiere tambien `STOCK`.
- Si una compra registra o revierte pago por caja, requiere tambien `CASH`.

Esto evita un agujero clasico: tener un modulo principal habilitado y usar funcionalidades de otro modulo por una integracion interna.

## Rendimiento

Las consultas del guard usan `SELECT EXISTS` y filtran por:

- tenant activo y no eliminado,
- producto,
- modulo,
- suscripcion activa,
- plan activo,
- entitlement activo,
- vigencia temporal.

El modelo persistente ya incluye indices para estas consultas. Si en el futuro el volumen o el patron de uso lo exige, el siguiente paso tecnico sera cachear capabilities por request o por tenant con invalidacion explicita ante cambios de licencia.

## Validacion

- `mvn -B test`: 110 pruebas unitarias, 0 fallos.
- `mvn -B verify`: 110 pruebas unitarias y 9 pruebas de integracion, 0 fallos.
- Flyway/Testcontainers/PostgreSQL 17.10: 20 migraciones hasta `v202607201500`.

## Fuera de alcance

- Administracion interna de licencias.
- UI de capabilities.
- Cache distribuida de capabilities.
- Auditoria de cambios de licencia.
- Billing real.
