# Sprint 3 - Functional Baseline

## Estado

Aprobado por Product Owner el 2026-07-20.

## Decision aprobada

Sprint 3 se define como: **Fundacion SaaS Comercial: Licencias, Modulos y Control de Acceso por Empresa**.

## Objetivo

Convertir la base multiempresa existente en una base SaaS comercialmente controlable: cada tenant debe tener productos, modulos, capacidades y limites definidos por configuracion, sin modificar codigo por cliente.

Sprint 3 no busca sumar mas funcionalidad ERP. Busca impedir que el ERP crezca como una aplicacion interna disfrazada de SaaS. Si una empresa no tiene contratado un modulo, el backend debe bloquearlo aunque el usuario tenga permisos. El frontend solo acompana; no decide seguridad.

## Problema a resolver

Hoy existen tablas fundacionales para productos, modulos y entitlements:

- `platform_products`
- `platform_modules`
- `tenant_product_entitlements`
- `tenant_module_entitlements`

Pero esos entitlements todavia no son una regla viva de ejecucion. Sprint 3 debe cerrar esa brecha.

La regla brutalmente simple: permiso sin modulo habilitado no alcanza. Modulo habilitado sin permiso tampoco alcanza.

## Principios de Sprint 3

- El backend es la fuente de verdad para licencias, modulos, permisos y capacidades.
- Ningun frontend puede habilitar modulos, productos, limites ni permisos por su cuenta.
- Ninguna API tenant-scoped acepta `tenant_id` libre desde frontend.
- El acceso se evalua por capas: tenant activo, producto habilitado, modulo habilitado y permiso RBAC.
- Deshabilitar un modulo nunca elimina datos historicos.
- Suspender o expirar una licencia bloquea operacion nueva, no borra informacion.
- Los codigos de productos, modulos, permisos, planes y limites deben ser estables y versionables.
- Los nombres comerciales pueden cambiar; los codigos tecnicos no deben cambiar una vez publicados.
- Toda modificacion de licencias, entitlements o limites debe auditarse.
- KODA como tenant piloto debe conservar acceso a todos los modulos existentes durante Sprint 3.
- El sistema debe quedar preparado para mas productos futuros: KODA POS, KODA CRM, KODA BI, KODA AI, KODA Mobile y KODA API.

## Terminologia aprobada

### Producto

Unidad comercial superior de la plataforma. Ejemplo inicial: `KODA_ERP`.

### Modulo

Capacidad funcional habilitable dentro de un producto. Ejemplos actuales: `CATALOGS`, `STOCK`, `COMMERCIAL_PARTNERS`, `CASH`, `SALES`, `PURCHASES`, `COMMERCIAL_REPORTS`.

### Plan

Plantilla comercial o tecnica que define un paquete de modulos y limites para un producto. Sprint 3 no define precios finales ni nombres publicos definitivos.

### Suscripcion

Relacion comercial vigente entre un tenant y un producto/plan.

### Entitlement

Derecho efectivo de uso de un producto o modulo para un tenant. Es la capa que el backend debe consultar para permitir o bloquear acceso.

### Limite

Regla cuantitativa asociada a un plan o tenant. Ejemplos candidatos: usuarios maximos, sucursales maximas, depositos maximos, productos maximos o retencion de auditoria.

### Feature flag

Interruptor tecnico de rollout o experimento. No reemplaza licencias ni permisos.

### Capability

Resultado calculado para un tenant: productos, modulos, features y limites realmente disponibles.

## Alcance aprobado

Sprint 3 incluye:

- Modelo funcional de planes, suscripciones, entitlements, limites y capabilities.
- Reutilizacion y endurecimiento de las tablas existentes de productos, modulos y entitlements.
- Mapeo de modulos actuales de KODA ERP contra permisos existentes.
- Guard backend reutilizable para validar producto/modulo antes o junto con RBAC.
- API tenant-scoped para consultar capabilities del tenant autenticado.
- APIs administrativas internas para consultar y administrar licencias/entitlements, protegidas por permisos de plataforma.
- Seed inicial para mantener KODA ERP activo en el tenant KODA.
- Preparacion frontend para leer capabilities y ocultar rutas/modulos no habilitados.
- Auditoria de cambios sobre suscripciones, entitlements y limites.
- Tests unitarios e integracion para activacion, suspension, expiracion y bloqueo por modulo.
- Documentacion tecnica y changelog actualizados.

## Reglas funcionales aprobadas

### Evaluacion de acceso

Para permitir una operacion tenant-scoped deben cumplirse todas estas condiciones:

1. El tenant existe y esta `ACTIVE`.
2. El producto requerido esta `ACTIVE` para el tenant.
3. El modulo requerido esta `ACTIVE` para el tenant.
4. La fecha actual esta dentro de la vigencia configurada.
5. El usuario tiene el permiso RBAC requerido para la accion.

Si falla producto o modulo, el error debe ser `403` con codigo estructurado especifico, por ejemplo `PRODUCT_NOT_ENABLED` o `MODULE_NOT_ENABLED`.

Si falla permiso, el error sigue siendo `PERMISSION_DENIED`.

### Producto inicial

El producto inicial sigue siendo `KODA_ERP`.

Sprint 3 no crea nuevos productos comerciales funcionales, pero deja la arquitectura preparada para incorporarlos.

### Modulos actuales de KODA ERP

Sprint 3 debe contemplar como minimo estos modulos:

- `SECURITY`
- `CONFIGURATION`
- `CATALOGS`
- `STOCK`
- `AUDIT`
- `COMMERCIAL_PARTNERS`
- `CASH`
- `SALES`
- `PURCHASES`
- `COMMERCIAL_REPORTS`

Los modulos core `SECURITY`, `CONFIGURATION` y `AUDIT` no deben exponerse como simples toggles comerciales para tenants activos. Pueden modelarse como modulos, pero no deben quedar apagables accidentalmente desde una pantalla operativa.

### Plan inicial

Se aprueba un plan tecnico inicial `KODA_PILOT` para el tenant KODA.

Este plan habilita KODA ERP y todos los modulos existentes. No representa precio comercial final ni packaging definitivo para clientes externos.

### Suscripciones y entitlements

- Una suscripcion activa debe materializar o resolver entitlements efectivos.
- Un entitlement suspendido o expirado bloquea operaciones del producto o modulo correspondiente.
- La suspension de un modulo no borra datos ya creados.
- La reactivacion de un modulo debe restaurar acceso a los datos existentes si el usuario tiene permisos.
- No se permite activar un modulo si el producto padre no esta activo para el tenant.
- No se permite eliminar fisicamente entitlements con historial; se debe cambiar estado y auditar.

### Limites

Sprint 3 puede modelar limites, pero no debe inventar precios ni reglas comerciales definitivas.

Limites candidatos iniciales:

- `MAX_USERS`
- `MAX_BRANCHES`
- `MAX_WAREHOUSES`
- `MAX_PRODUCTS`
- `AUDIT_RETENTION_DAYS`

La aplicacion estricta de cada limite debe implementarse solo donde exista una forma confiable de medirlo sin degradar rendimiento.

### Feature flags

- Feature flags sirven para rollout tecnico, pruebas controladas o activacion gradual.
- Feature flags no sustituyen entitlements comerciales.
- Una feature flag habilitada no puede abrir un modulo sin licencia.

### Frontend

- El frontend debe consultar capabilities despues del login y seleccion de tenant.
- Las rutas, menus y acciones de modulos deshabilitados deben ocultarse o bloquearse.
- El frontend nunca se considera mecanismo de seguridad suficiente.
- Si el backend bloquea un modulo, el frontend debe mostrar un mensaje claro y no tecnico.

### Administracion

- Tenant owners/admins pueden consultar capacidades del propio tenant.
- Tenant owners/admins no pueden auto-habilitar modulos pagos en Sprint 3.
- La administracion de licencias queda reservada a permisos de plataforma.
- Toda modificacion administrativa debe registrar auditoria.

## Matriz inicial modulo-permiso

| Modulo | Permisos principales |
| --- | --- |
| `SECURITY` | `users:*`, `roles:*` |
| `CONFIGURATION` | `tenants:*`, `branches:*`, `company_settings:*` |
| `CATALOGS` | `products:*`, `brands:*`, `categories:*`, `units:*`, `presentations:*` |
| `STOCK` | `stock_movements:*` |
| `AUDIT` | `audit:read` |
| `COMMERCIAL_PARTNERS` | `customers:*`, `suppliers:*` |
| `CASH` | `cash_registers:*`, `cash_sessions:*`, `cash_movements:*` |
| `SALES` | `sales:*` |
| `PURCHASES` | `purchases:*` |
| `COMMERCIAL_REPORTS` | `commercial_reports:read` |

Esta matriz puede ampliarse, pero no debe romper permisos existentes sin aprobacion explicita.

## Fuera de alcance de Sprint 3

- Pasarela de pagos.
- Facturacion a clientes SaaS.
- Precios comerciales finales.
- Portal self-service de upgrade/downgrade.
- Marketplace de modulos.
- Medicion avanzada de uso para billing.
- UI completa de todos los modulos ERP.
- Nuevos modulos ERP funcionales.
- Facturacion fiscal, contabilidad o impuestos.
- Multi-producto real mas alla de dejar la arquitectura preparada.

## Criterios de aceptacion funcional

Sprint 3 se considerara funcionalmente aceptable si:

- KODA conserva acceso a todos los modulos existentes bajo `KODA_PILOT`.
- Un modulo deshabilitado bloquea la API aunque el usuario tenga permisos.
- Un modulo habilitado no concede acceso si el usuario no tiene permisos.
- Las capabilities del tenant se pueden consultar desde API.
- El frontend puede usar capabilities para construir navegacion y rutas disponibles.
- Las modificaciones de licencias/entitlements quedan auditadas.
- Las migraciones son reproducibles en PostgreSQL 17.
- Existen tests para producto activo, producto suspendido, modulo activo, modulo suspendido, modulo expirado y permiso insuficiente.
- README, roadmap, changelog y documentacion especifica quedan actualizados.

## Decision

Se aprueba esta base funcional para iniciar Sprint 3.

Cualquier cambio sobre planes, suscripciones, entitlements, limites, feature flags, matriz modulo-permiso o reglas de bloqueo requiere aprobacion explicita del Product Owner antes de implementarse.