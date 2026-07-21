# Tenant License Administration

## Estado

Implementado en Sprint 3 Hito 5.

## Objetivo

Permitir que KODA PLATFORM administre internamente las licencias de una empresa sin exponer autoservicio comercial al tenant.

Esta API es de plataforma, no del ERP operativo.

## Endpoints internos

Base path:

- `GET /api/v1/platform/tenants/{tenantId}/licenses`
- `PATCH /api/v1/platform/tenants/{tenantId}/licenses/subscriptions/{subscriptionId}`
- `PATCH /api/v1/platform/tenants/{tenantId}/licenses/product-entitlements/{entitlementId}`
- `PATCH /api/v1/platform/tenants/{tenantId}/licenses/module-entitlements/{entitlementId}`

## Permisos

Se agregan permisos de plataforma:

- `license_admin:read`
- `license_admin:update`

Regla de seguridad:

- El actor debe ser `platform_admin = true`.
- El actor debe tener el permiso requerido.
- Un usuario tenant, aunque tenga permisos RBAC operativos, no puede administrar licencias.

## Reglas de actualizacion

### Suscripciones

Estados permitidos:

- `ACTIVE`
- `SUSPENDED`
- `EXPIRED`
- `CANCELLED`

Las actualizaciones usan `version` optimista para evitar pisadas concurrentes.

### Entitlements de producto

Estados permitidos:

- `ACTIVE`
- `SUSPENDED`
- `EXPIRED`

### Entitlements de modulo

Estados permitidos:

- `ACTIVE`
- `SUSPENDED`
- `EXPIRED`

Los modulos `core_module` o no `commercially_toggleable` no pueden suspenderse ni recibir vencimiento comercial desde esta API.

Motivos de rechazo:

- `CORE_MODULE_PROTECTED`
- `MODULE_NOT_COMMERCIALLY_TOGGLEABLE`
- `INVALID_SUBSCRIPTION_STATUS`
- `INVALID_ENTITLEMENT_STATUS`
- `INVALID_VALIDITY_WINDOW`
- `INVALID_VERSION`

## Auditoria

Todo cambio registra evento en `audit_events` con:

- tenant afectado,
- actor,
- accion,
- recurso,
- estado anterior,
- estado nuevo,
- version anterior,
- version nueva.

Acciones actuales:

- `license.subscription.update`
- `license.product_entitlement.update`
- `license.module_entitlement.update`

## Respuestas de error

- `403 PERMISSION_DENIED`: actor no es administrador de plataforma o no tiene permiso.
- `404 TENANT_LICENSE_ADMIN_NOT_FOUND`: tenant o recurso inexistente.
- `409 TENANT_LICENSE_ADMIN_VERSION_CONFLICT`: version optimista desactualizada.
- `409 TENANT_LICENSE_ADMIN_OPERATION_REJECTED`: regla de licencia bloquea la operacion.

## Fuera de alcance

- Self-service comercial del tenant.
- Billing real.
- Precios.
- Facturas SaaS.
- Pasarela de pago.
- Historial enriquecido de auditoria como pantalla.

## Validacion

- Pruebas unitarias de reglas de administracion de licencias.
- Pruebas Flyway/Testcontainers para permisos `license_admin:*`.
- Prueba de lectura real de la administracion del tenant KODA sobre PostgreSQL 17.
