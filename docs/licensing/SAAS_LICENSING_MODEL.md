# SaaS Licensing Model

## Estado

Modelo persistente implementado en Sprint 3 Hito 2. Calculo backend y API de capabilities implementados en Sprint 3 Hito 3. Guards backend por producto/modulo implementados en Sprint 3 Hito 4. Administracion interna de licencias implementada en Sprint 3 Hito 5. Shell frontend de capabilities implementado en Sprint 3 Hito 6.

## Objetivo

Definir la base persistente para que KODA PLATFORM pueda controlar productos, planes, modulos, suscripciones, limites, feature flags y entitlements por tenant.

Esta capa no reemplaza RBAC. La complementa.

Regla central: un usuario necesita permiso y el tenant necesita derecho comercial de uso. Una sola de esas condiciones no alcanza.

## Migracion

Modelo creado en:

- `backend/src/main/resources/db/migration/V202607201500__create_saas_licensing_model.sql`
- `backend/src/main/resources/db/migration/V202607201600__seed_license_administration_permissions.sql`

La migracion se apoya sobre tablas ya existentes:

- `platform_products`
- `platform_modules`
- `tenant_product_entitlements`
- `tenant_module_entitlements`

## Tablas incorporadas

### `product_plans`

Define planes por producto. En Sprint 3 se crea el plan tecnico `KODA_PILOT` para `KODA_ERP`.

Campos clave:

- `product_id`
- `code`
- `name`
- `status`
- `is_system`
- `version`

### `product_plan_modules`

Relaciona planes con modulos incluidos.

Permite calcular que modulos deberia tener disponible un tenant segun su plan.

### `product_plan_limits`

Define limites por plan.

Limites iniciales modelados:

- `MAX_USERS`
- `MAX_BRANCHES`
- `MAX_WAREHOUSES`
- `MAX_PRODUCTS`
- `AUDIT_RETENTION_DAYS`

Para `KODA_PILOT` se registran como ilimitados. Esto evita inventar restricciones comerciales antes de tiempo y conserva al tenant KODA como piloto completo.

### `tenant_product_subscriptions`

Registra la suscripcion vigente de un tenant a un producto y plan.

Estados soportados:

- `ACTIVE`
- `SUSPENDED`
- `EXPIRED`
- `CANCELLED`

La migracion crea una suscripcion activa para KODA sobre `KODA_ERP` con plan `KODA_PILOT`.

### `tenant_limit_overrides`

Permite sobrescribir limites de plan para un tenant especifico.

No se cargan overrides iniciales.

### `tenant_feature_flags`

Permite activar o desactivar flags tecnicos por tenant, producto y opcionalmente modulo.

No reemplaza licencias ni permisos. Una feature flag habilitada no debe abrir un modulo sin entitlement activo.

## Ajustes sobre tablas existentes

### `platform_modules`

Se agregan:

- `core_module`
- `commercially_toggleable`

Los modulos `SECURITY`, `CONFIGURATION` y `AUDIT` quedan marcados como core y no toggleables comercialmente.

## Seed inicial

Se crea:

- Plan `KODA_PILOT` para `KODA_ERP`.
- Asociacion de `KODA_PILOT` con los 10 modulos actuales.
- 5 limites ilimitados para `KODA_PILOT`.
- Suscripcion activa de tenant KODA a `KODA_ERP` con `KODA_PILOT`.
- Insercion defensiva de entitlements de producto y modulos para KODA si faltaran.

## Indices

Se agregan indices para preparar consultas eficientes de capabilities:

- `idx_product_plans_product_status`
- `idx_product_plan_modules_product`
- `idx_product_plan_limits_plan`
- `uq_tenant_product_subscriptions_current`
- `idx_tenant_product_subscriptions_tenant_status`
- `idx_tenant_product_subscriptions_plan`
- `idx_tenant_limit_overrides_tenant_product`
- `uq_tenant_feature_flags_scope_code`
- `idx_tenant_feature_flags_tenant_product_enabled`
- `idx_tenant_product_entitlements_tenant_status`
- `idx_tenant_module_entitlements_tenant_status`

## Capabilities backend

Implementado en Hito 3 mediante:

- Servicio `TenantCapabilitiesService`.
- Puerto `TenantCapabilitiesRepository`.
- Repositorio `JdbcTenantCapabilitiesRepository`.
- Endpoint `GET /api/v1/capabilities`.

La respuesta expone:

- tenant activo,
- productos habilitados,
- modulos habilitados por producto,
- feature flags efectivas,
- limites efectivos,
- timestamp de calculo.

Reglas de calculo actuales:

- filtra siempre por tenant,
- exige suscripcion activa y vigente,
- exige plan activo,
- exige producto y modulo activos,
- exige entitlement activo de producto y modulo,
- respeta `valid_from` y `valid_until`,
- resuelve overrides de limites por tenant sobre los limites del plan.

La API requiere usuario autenticado y tenant context. No exige permiso RBAC especifico porque el frontend necesita leer capabilities para construir navegacion y rutas.

## Guards backend

Implementado en Hito 4 mediante `TenantLicenseAccessGuard` y documentado en `docs/licensing/TENANT_LICENSE_GUARDS.md`.

El guard bloquea operaciones cuando el tenant no tiene producto o modulo habilitado, aunque el usuario tenga permisos RBAC. Los errores API son 403 con codigo `TENANT_LICENSE_ACCESS_DENIED` y motivos `PRODUCT_NOT_ENABLED` o `MODULE_NOT_ENABLED`.

## Administracion interna

Implementada en Hito 5 y documentada en `docs/licensing/TENANT_LICENSE_ADMINISTRATION.md`.

Endpoints internos:

- `GET /api/v1/platform/tenants/{tenantId}/licenses`
- `PATCH /api/v1/platform/tenants/{tenantId}/licenses/subscriptions/{subscriptionId}`
- `PATCH /api/v1/platform/tenants/{tenantId}/licenses/product-entitlements/{entitlementId}`
- `PATCH /api/v1/platform/tenants/{tenantId}/licenses/module-entitlements/{entitlementId}`

Permisos:

- `license_admin:read`
- `license_admin:update`

La administracion exige actor de plataforma y permiso explicito. No habilita self-service comercial para tenants.

## Shell frontend

Implementado en Hito 6 y documentado en `docs/licensing/FRONTEND_CAPABILITY_SHELL.md`.

El frontend carga `GET /api/v1/capabilities`, construye navegacion a partir de modulos habilitados y bloquea visualmente rutas directas a modulos sin licencia activa.

Esta capa mejora experiencia operativa, pero no reemplaza seguridad backend.

## Validacion

Validado con:

- `mvn -B test`: 115 tests unitarios, 0 fallos.
- `mvn -B verify`: 115 tests unitarios y 11 tests de integracion, 0 fallos.
- Flyway/Testcontainers/PostgreSQL 17.10: 21 migraciones hasta `v202607201600`.
- `npm.cmd run test`: 3 tests frontend, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos.

## Fuera de alcance actual

- UI completa de operacion comercial.
- Billing real.
- Precios comerciales finales.

## Siguiente paso

Hito 7 debe cerrar hardening de Sprint 3: validacion completa, documentacion final y reporte de cierre.
